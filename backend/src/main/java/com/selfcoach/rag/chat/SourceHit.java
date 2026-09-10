package com.selfcoach.rag.chat;

/**
 * 一条引用来源（第 7 步回答的「带引用」载体）。
 * 对应检索命中的一块知识片段，字段与契约一致：docId + chunkIndex + content。
 * 有意不带 score（那是检索阶段的内部排序分，对最终回答的引用展示无意义）。
 */
public class SourceHit {

    /** 文档唯一标识（对应 doc_chunk.doc_id） */
    private final String docId;

    /** 该文档内的分块序号（对应 doc_chunk.chunk_index） */
    private final int chunkIndex;

    /** 分块原文（让用户能直接核对「回答依据了哪段原文」） */
    private final String content;

    public SourceHit(String docId, int chunkIndex, String content) {
        this.docId = docId;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    public String getDocId() { return docId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
}