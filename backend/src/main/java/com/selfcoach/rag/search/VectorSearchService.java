package com.selfcoach.rag.search;

import com.selfcoach.llm.EmbeddingClient;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 向量检索（第 6 步第二块）：把查询文本向量化，在 doc_chunk 里按余弦相似度查 top 候选。
 *
 * <p>技术难点与解法：doc_chunk.embedding 是 pgvector 的 vector 类型，r2dbc-postgresql
 * 不直接支持，所以不能走 R2DBC 实体映射，必须走 {@link DatabaseClient} 原生 SQL。
 * 把 1024 维 float[] 拼成文本 {@code [a,b,c,...]}，再用 {@code :q::vector} 让 PostgreSQL
 * 自己完成「文本 → vector」类型转换——这与第 5 步入库写向量的手法完全对称。</p>
 *
 * <p>相似度怎么算：pgvector 的 {@code <=>} 操作符是「余弦距离」，值域 [0,2]，越小越像
 * （0 = 方向完全相同，2 = 完全相反）。所以相似度 = {@code 1 - (embedding <=> :q::vector)}，
 * 这样分数越大表示越相关，和 BM25 分「越大越相关」的直觉统一，后续融合才不会符号打架。</p>
 *
 * <p>为什么这就是「向量检索」而非自己手写 ANN：建表时已按 HNSW + cosine 建好索引
 * （见 schema.sql），这里只需一条 ORDER BY <=> 交给 pgvector 内部走索引，无需重造轮子。</p>
 */
@Service
public class VectorSearchService {

    private final EmbeddingClient embedClient;
    private final DatabaseClient db;

    public VectorSearchService(EmbeddingClient embedClient, DatabaseClient db) {
        this.embedClient = embedClient;
        this.db = db;
    }

    /**
     * 按语义相似度检索：q → 向量 → pgvector 余弦相似度 topN。
     * score = 1 - 余弦距离（越大越相关）。
     */
    public Mono<List<SearchHit>> search(String queryText, int topN) {
        if (queryText == null || queryText.isBlank()) {
            return Mono.just(List.of());
        }
        return embedClient.embed(queryText)
                .flatMap(vec -> queryDb(vec, topN));
    }

    /** 用向量去 pgvector 里查最近邻。 */
    private Mono<List<SearchHit>> queryDb(float[] vec, int topN) {
        String vectorText = toVectorText(vec);
        return db.sql("""
                        SELECT doc_id, chunk_index, content,
                               1 - (embedding <=> :q::vector) AS similarity
                        FROM doc_chunk
                        ORDER BY embedding <=> :q::vector
                        LIMIT :k
                        """)
                .bind("q", vectorText)
                .bind("k", topN)
                .map((row, meta) -> new SearchHit(
                        row.get("doc_id", String.class),
                        ((Number) row.get("chunk_index")).intValue(),
                        row.get("content", String.class),
                        ((Number) row.get("similarity")).doubleValue()))
                .all()
                .collectList();
    }

    /** float[] → "[a,b,c]" 向量文本；小数固定 6 位，避免科学计数法被 pgvector 拒掉。 */
    private String toVectorText(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%.6f", v[i]));
        }
        return sb.append(']').toString();
    }
}