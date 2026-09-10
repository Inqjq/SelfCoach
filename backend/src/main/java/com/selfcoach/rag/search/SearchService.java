package com.selfcoach.rag.search;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索服务（第 6 步第三块）：把 BM25 与向量两路候选做 RRF 融合，输出最终命中列表。
 *
 * <p>为什么需要融合：BM25 擅长「关键词精确命中」（如搜「深蹲」能锁定含该词的块），
 * 向量检索擅长「语义近似」（换种说法也能命中）。单靠任何一路都有盲区——
 * 只靠 BM25 会漏掉「同义改写」的块；只靠向量会漏掉「专有名词必须精确出现」的块。
 * RRF 把两路的排名信息合并，相互补盲，是业界（如 Elasticsearch 8 的 hybrid search）主流做法。</p>
 *
 * <p>为什么 RRF 而非把两路分数直接加权相加：BM25 分和余弦相似度的量纲不同、分布不同，
 * 直接相加要先做归一化（min-max 等），引入了额外敏感假设；RRF 只用「排名」而非「分数」，
 * 对各自分数尺度完全不敏感，实现更稳、更少超参。公式：score(d)=Σ 1/(k+rank)，k 通常取 60。</p>
 */
@Service
public class SearchService {

    /** RRF 平滑常数 k：越大排名靠前的优势被拉得越平，60 是社区常用值 */
    private static final double RRF_K = 60.0;

    /** 每路先取的候选数，再在这 2×N 个候选上做融合（避免了在全表上融合的开销） */
    private static final int CANDIDATE_N = 20;

    /** 最后返回给前端的命中条数 */
    private static final int DEFAULT_TOP_K = 5;

    private final LuceneIndexService lucene;
    private final VectorSearchService vector;

    public SearchService(LuceneIndexService lucene, VectorSearchService vector) {
        this.lucene = lucene;
        this.vector = vector;
    }

    /**
     * 入口：并行跑两路检索 → RRF 融合 → 按融合分降序取 topK。
     */
    public Mono<List<SearchHit>> search(String q, int topK) {
        int k = topK > 0 ? topK : DEFAULT_TOP_K;

        // Mono.zip 会同时订阅两路（BM25 走 boundedElastic、向量走 WebClient+r2dbc），并发取候选。
        Mono<List<SearchHit>> bm25Hits = lucene.search(q, CANDIDATE_N);
        Mono<List<SearchHit>> vectorHits = vector.search(q, CANDIDATE_N);

        return Mono.zip(bm25Hits, vectorHits)
                .map(tuple -> fuse(tuple.getT1(), tuple.getT2(), k));
    }

    /** RRF 融合核心：两路候选按 (docId, chunkIndex) 合并，累加 1/(k+rank)，降序取 topK。 */
    private List<SearchHit> fuse(List<SearchHit> bm25, List<SearchHit> vec, int topK) {
        // byKey 保存每个 chunk 的身份（docId/chunkIndex/content），LinkedHashMap 保证后续迭代顺序稳定
        Map<String, SearchHit> byKey = new LinkedHashMap<>();
        // rrfScore 累加每个 chunk 的 RRF 分
        Map<String, Double> rrfScore = new HashMap<>();

        accumulate(bm25, byKey, rrfScore);
        accumulate(vec, byKey, rrfScore);

        List<SearchHit> merged = new ArrayList<>(byKey.size());
        for (SearchHit hit : byKey.values()) {
            String key = hit.key();
            merged.add(new SearchHit(
                    hit.getDocId(), hit.getChunkIndex(), hit.getContent(), rrfScore.get(key)));
        }
        // RRF 分越大越相关，降序排列
        merged.sort(Comparator.comparingDouble(SearchHit::getScore).reversed());

        // 只取前 topK（不足 topK 就取全部）
        int end = Math.min(topK, merged.size());
        return new ArrayList<>(merged.subList(0, end));
    }

    /** 把一路候选按 rank(从 1 起) 折成 RRF 分累加进去。 */
    private void accumulate(List<SearchHit> ranked, Map<String, SearchHit> byKey, Map<String, Double> rrfScore) {
        for (int i = 0; i < ranked.size(); i++) {
            SearchHit hit = ranked.get(i);
            String key = hit.key();
            // 同一块两路都命中时只留一份身份信息（内容本就读自同一行）
            byKey.putIfAbsent(key, hit);
            // 第 i+1 名贡献 1/(k + rank)。两路都命中同一块时分数自然累加（这就是融合的意义）
            rrfScore.merge(key, 1.0 / (RRF_K + (i + 1)), Double::sum);
        }
    }
}