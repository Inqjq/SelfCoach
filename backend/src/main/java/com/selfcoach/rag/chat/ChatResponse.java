package com.selfcoach.rag.chat;

import java.util.List;

/**
 * 问答接口 POST /api/chat 的响应体：
 *   answer  —— LLM 生成的回答文本（引用处用 [n] 标注对应 sources[n-1]）
 *   sources —— 本回答所依据的知识片段列表（docId + chunkIndex + content）
 */
public class ChatResponse {

    private final String answer;
    private final List<SourceHit> sources;

    public ChatResponse(String answer, List<SourceHit> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() { return answer; }
    public List<SourceHit> getSources() { return sources; }
}