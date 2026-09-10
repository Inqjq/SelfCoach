package com.selfcoach.rag.search;

/**
 * 检索命中结果（第 6 步对外返回的基本单元）。
 *
 * <p>三块（BM25 / 向量 / 融合）都用它当「一块命中」的载体：前两路各自产出它，
 * 融合阶段把两路按 (docId, chunkIndex) 合并后重算 score。最终 GET /api/search 输出它。</p>
 *
 * <p>score 语义：融合后的 RRF 分（第 3 块算）；在前两路中间态里，score 先放各自的原始分
 * （BM25 分 / 余弦相似度），方便调试打印，融合时会覆盖。</p>
 */
public class SearchHit {

    /** 文档唯一标识（对应 doc_chunk.doc_id），用来回溯源文档 */
    private final String docId;

    /** 该文档内的分块序号（对应 doc_chunk.chunk_index），docId+chunkIndex 唯一定位一块 */
    private final int chunkIndex;

    /** 分块正文（返回给前端做展示/给 LLM 当上下文） */
    private final String content;

    /** 相关性分数（融合后为 RRF 分，越大越相关） */
    private final double score;

    public SearchHit(String docId, int chunkIndex, String content, double score) {
        this.docId = docId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.score = score;
    }

    public String getDocId() { return docId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public double getScore() { return score; }

    /** RRF 融合的去重 key：同一篇文档的同一块，无论从哪路命中都算同一个 */
    public String key() {
        return docId + "|" + chunkIndex;
    }
}