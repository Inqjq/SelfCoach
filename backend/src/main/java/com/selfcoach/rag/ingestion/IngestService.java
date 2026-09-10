package com.selfcoach.rag.ingestion;

import com.selfcoach.llm.EmbeddingClient;
import com.selfcoach.rag.search.LuceneIndexService;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 入库服务（RAG 入库管线核心）：把一篇文档 → 分块 → 每个块向量化 → 写入 doc_chunk 表。
 *
 * <p>技术难点：r2dbc-postgresql 默认不支持 pgvector 的 vector 类型列读写，所以这里不能用
 * R2DBC 实体自动映射，而是走 {@link DatabaseClient} 原生 SQL，把向量拼成字符串 `[a,b,c]`
 * 再用 `:embedding::vector` 交给 PostgreSQL 的类型转换完成写入。</p>
 */
@Service
public class IngestService {

    private final EmbeddingClient embedClient;
    private final DatabaseClient db;
    private final DocumentChunker chunker;
    private final LuceneIndexService luceneIndex;

    public IngestService(EmbeddingClient embedClient, DatabaseClient db, LuceneIndexService luceneIndex) {
        this.embedClient = embedClient;
        this.db = db;
        this.chunker = new DocumentChunker(600);
        this.luceneIndex = luceneIndex;
    }

    /**
     * 入库：返回成功写入的分块数。
     * 流程：分块 → 逐块调用 embedding → 原生 SQL 写入 doc_chunk。
     */
    public Mono<Integer> ingest(String docId, String source, String document) {
        List<String> chunks = chunker.chunk(document);
        return Flux.range(0, chunks.size())
                .concatMap(i -> embedClient.embed(chunks.get(i))
                        .flatMap(vec -> insert(docId, i, chunks.get(i), vec, source).thenReturn(1)))
                .reduce(0, Integer::sum)
                // 入库成功后失效 Lucene 内存索引，下次检索重建，保证「入库 → 检索」能立刻看到新数据
                .doOnSuccess(n -> { if (n != null && n > 0) luceneIndex.invalidate(); });
    }

    /** 用原生 SQL 写入一行 doc_chunk。embedding 以 `[...]::vector` 形式交给 pgvector。 */
    private Mono<Void> insert(String docId, int idx, String content, float[] vec, String source) {
        return db.sql("""
                INSERT INTO doc_chunk (doc_id, chunk_index, content, embedding, source)
                VALUES (:docId, :idx, :content, :embedding::vector, :source)
                """)
                .bind("docId", docId)
                .bind("idx", idx)
                .bind("content", content)
                .bind("embedding", toVectorText(vec))
                .bind("source", source)
                .then();
    }

    /** float[] → "[a,b,c]" 向量文本。小数固定 6 位，避免科学计数法被 pgvector 拒掉。 */
    private String toVectorText(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%.6f", v[i]));
        }
        return sb.append(']').toString();
    }
}