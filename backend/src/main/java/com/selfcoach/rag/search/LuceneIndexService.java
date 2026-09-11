package com.selfcoach.rag.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * BM25 关键词检索（第 6 步第一块）：用 Lucene 给 doc_chunk.content 建内存倒排索引。
 *
 * <p>为什么用 Lucene 而不是自己写 BM25：BM25 的「词频/逆文档频率/长度归一」公式不难，
 * 但倒排索引本身要做分词、词典、posting list，工程量大且易错；项目允许「基础设施用现成库」，
 * Lucene 是业界最成熟的纯 Java 实现，所以这里用库、检索的编排与融合逻辑仍自己手写。</p>
 *
 * <p>几个关键实现决策：</p>
 * <ol>
 *   <li><b>目录用 {@link ByteBuffersDirectory}</b>：任务提的 RAMDirectory 在 Lucene 9.0 已移除，
 *       9.x 的内存目录实现就是 ByteBuffersDirectory，纯内存、不落盘，语义等价。</li>
 *   <li><b>分析器用 {@link StandardAnalyzer}</b>（lucene-core 自带，不额外引依赖）：
 *       英文按词切；中文按 UAX#29 规则「逐字」切（每个汉字一个 token）。
 *       好处：零依赖、能直接跑通；局限：中文只有单字粒度，没有词典分词。
 *       演进方向是换 lucene-analysis-smartcn / ik-analyzer，起步先用它满足验收。</li>
 *   <li><b>索引懒加载 + 缓存</b>：首次检索时从 doc_chunk 全量加载建一次内存索引，
 *       之后复用；个人知识库数据量小，重建成本可忽略。入库后调 {@link #invalidate()} 失效重建。</li>
 *   <li><b>查询解析用 QueryParser + escape</b>：先转义用户输入里的特殊字符（+ - : ( 等），
 *       避免把普通词误当成 Lucene 表达式语法报错；多词默认 OR，单字都能进候选，BM25 再排序。</li>
 * </ol>
 */
@Service
public class LuceneIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexService.class);

    private static final String FIELD_DOC_ID = "docId";
    private static final String FIELD_CHUNK = "chunkIndex";
    private static final String FIELD_CONTENT = "content";

    private final DatabaseClient db;
    private final Analyzer analyzer = new StandardAnalyzer();

    /** 内存索引；null 表示尚未构建（下次检索时懒加载）。volatile 保证多线程可见。 */
    private volatile Directory directory;

    public LuceneIndexService(DatabaseClient db) {
        this.db = db;
    }

    /**
     * BM25 检索：返回按相关度降序的 topN 命中。
     * score 用 Lucene 的原始 BM25 分（仅作调试；融合阶段会改用 RRF 分覆盖）。
     */
    public Mono<List<SearchHit>> search(String queryText, int topN) {
        // 空查询直接返回空，不建索引、不查库。
        if (queryText == null || queryText.isBlank()) {
            return Mono.just(List.of());
        }
        return ensureBuilt()
                // Lucene 的 open/query/close 是阻塞 IO，放到独立线程池，不占 reactor 事件循环线程。
                .flatMap(dir -> Mono.fromCallable(() -> doSearch(dir, queryText, topN))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /** 入库后调用：把内存索引标记失效，下次检索时从最新的 doc_chunk 重建。 */
    public void invalidate() {
        directory = null;
    }

    // ---- 以下为内部实现 ----

    /** 拿内存索引；没有则从 doc_chunk 全量加载构建。 */
    private Mono<Directory> ensureBuilt() {
        Directory d = directory;
        if (d != null) {
            return Mono.just(d);
        }
        return loadChunks()
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .map(chunks -> {
                    Directory built = buildIndex(chunks);
                    directory = built;
                    return built;
                });
    }

    /** 从库里读出所有分块（只需建索引用的三个字段）。 */
    private Flux<Chunk> loadChunks() {
        return db.sql("SELECT doc_id, chunk_index, content FROM doc_chunk ORDER BY doc_id, chunk_index")
                .map((row, meta) -> new Chunk(
                        row.get("doc_id", String.class),
                        ((Number) row.get("chunk_index")).intValue(),
                        row.get("content", String.class)))
                .all();
    }

    /** 把一批分块写进内存倒排索引，返回可搜索的 Directory。 */
    private Directory buildIndex(List<Chunk> chunks) {
        try {
            Directory dir = new ByteBuffersDirectory();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            // try-with-resources：close 时会 commit 并落索引到 dir。
            try (IndexWriter writer = new IndexWriter(dir, config)) {
                for (Chunk c : chunks) {
                    Document doc = new Document();
                    doc.add(new StoredField(FIELD_DOC_ID, c.docId()));          // 只存不索引，用于回溯源文档
                    doc.add(new StoredField(FIELD_CHUNK, String.valueOf(c.chunkIndex()))); // 同上
                    // content：TextField = 分词 + 建倒排（检索用），但不存储正文（Store.NO，省内存）
                    doc.add(new TextField(FIELD_CONTENT, c.content(), Field.Store.NO));
                    // 正文另外用 StoredField 存一份，命中后要原样返回给前端
                    doc.add(new StoredField(FIELD_CONTENT, c.content()));
                    writer.addDocument(doc);
                }
            }
            return dir;
        } catch (IOException e) {
            // 索引构建失败属于不应发生的异常，包装成运行时异常直接抛出（让上层感知，而不是悄悄吞掉）
            throw new UncheckedIOException("Lucene 索引构建失败", e);
        }
    }

    /** 在已有的索引上执行一次查询，返回降序命中列表。 */
    private List<SearchHit> doSearch(Directory dir, String queryText, int topN) {
        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader); // 默认 BM25Similarity，正是我们要的
            QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);
            Query query = parser.parse(QueryParserBase.escape(queryText));
            TopDocs topDocs = searcher.search(query, topN);

            List<SearchHit> hits = new ArrayList<>(topDocs.scoreDocs.length);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                hits.add(new SearchHit(
                        d.get(FIELD_DOC_ID),
                        Integer.parseInt(d.get(FIELD_CHUNK)),
                        d.get(FIELD_CONTENT),
                        sd.score));
            }
            return hits;
        } catch (Exception e) {
            // 兜底：单次查询语法/解析异常不拖垮整个搜索接口，记日志并返回空结果。
            log.warn("Lucene 查询失败 query={}, err={}", queryText, e.getMessage());
            return List.of();
        }
    }

    /** 分块的最小记录：从 doc_chunk 读出的建索引原料。 */
    private record Chunk(String docId, int chunkIndex, String content) {}
}