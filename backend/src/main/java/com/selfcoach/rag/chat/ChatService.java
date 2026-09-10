package com.selfcoach.rag.chat;

import com.selfcoach.rag.search.SearchHit;
import com.selfcoach.rag.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 问答服务（第 7 步 / F-03 最后一环）：检索 → 拼提示词 → 生成 → 带引用返回。
 *
 * <p>管线四步：</p>
 * <ol>
 *   <li>用 {@link SearchService} 对问题做混合检索，取 top-K 片相关分块；</li>
 *   <li>把片段编上号 [1..k] 拼进用户提示词，system 指令限定「只能依据知识作答，没有就说不确定」；</li>
 *   <li>调 {@link ChatClient} 生成回答；</li>
 *   <li>把「本回答所依据的检索命中断」映射成引用来源 docId+chunkIndex+content 一并返回。</li>
 * </ol>
 *
 * <p>为什么 sources 返回「全部 top-K 命中」而非只解析 LLM 标注出的编号：LLM 只能依据
 * prompt 里的这 K 段作答，它们就是回答的全部依据；而用正则去解析 LLM 输出里的 [n] 标注
 * 格式脆弱、易漏（漏配会导致「带了引用但 sources 为空」）。所以这里用编号让 answer 与
 * sources 下标对应（sources[i] ↔ [i+1]），引用真实可查，既稳又满足验收。</p>
 *
 * <p>降级策略（保证接口不 500）：检索无结果 → 直接告知无相关内容（不烧 token）；
 * LLM 调用异常 → 返回「无法回答」提示。两种都走正常 HTTP 200。</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 检索 top-K：默认取 5 段放进上下文 */
    private static final int TOP_K = 5;

    /** 统一的安全边界指令：限定只依据知识、不编造、标注来源 */
    private static final String SYSTEM_PROMPT = """
            你是 SelfCoach（自律健身教练）的知识问答助手。请严格遵守：
            1. 只能依据下方「知识片段」回答，不得使用你的固有知识，不得编造任何事实；
            2. 如果知识片段里没有足够信息回答，请直接说「根据现有知识无法确定」，不要猜测或扩展；
            3. 引用了某段知识时，请在相关句子末尾标注来源编号，如 [1]、[2]；
            4. 用中文，回答简洁、专业、口语化。
            """;

    private static final String EMPTY_QUESTION = "请先输入你的问题。";
    private static final String NO_KNOWLEDGE = "抱歉，知识库中没有找到与这个问题相关的内容，无法回答。";
    private static final String MODEL_UNAVAILABLE = "抱歉，我暂时无法生成回答，请稍后再试。";

    private final SearchService searchService;
    private final ChatClient chatClient;

    public ChatService(SearchService searchService, ChatClient chatClient) {
        this.searchService = searchService;
        this.chatClient = chatClient;
    }

    /**
     * 入口：问题 → 回答 + 引用来源。
     * 全程 Reactor 响应式，各步用 flatMap/map 串联，不阻塞事件循环。
     */
    public Mono<ChatResponse> answer(String question) {
        if (question == null || question.isBlank()) {
            return Mono.just(new ChatResponse(EMPTY_QUESTION, List.of()));
        }

        return searchService.search(question, TOP_K)
                .flatMap(hits -> {
                    // 检索完全没命中 → 直接拒答，不浪费一轮 LLM 调用
                    if (hits.isEmpty()) {
                        return Mono.just(new ChatResponse(NO_KNOWLEDGE, List.of()));
                    }
                    String userPrompt = buildPrompt(hits, question);
                    return chatClient.chat(SYSTEM_PROMPT, userPrompt)
                            .map(answer -> new ChatResponse(blankToFallback(answer), toSources(hits)))
                            // LLM 调用失败（网络/超时/非2xx/解析异常）→ 降级，返回固定提示而非 500
                            .onErrorResume(e -> {
                                log.warn("LLM 生成失败 question={}, err={}", question, e.getMessage());
                                return Mono.just(new ChatResponse(MODEL_UNAVAILABLE, List.of()));
                            });
                });
    }

    /** 把 top-K 片段编号拼成用户消息。编号从 [1] 起，对应最终 sources 下标。 */
    private String buildPrompt(List<SearchHit> hits, String question) {
        StringBuilder sb = new StringBuilder("知识片段：\n");
        for (int i = 0; i < hits.size(); i++) {
            sb.append('[').append(i + 1).append("] ").append(hits.get(i).getContent()).append('\n');
        }
        sb.append("\n问题：").append(question);
        return sb.toString();
    }

    /** 把检索命中转成引用来源（丢弃检索用的 score，只留对外要展示的三个字段）。 */
    private List<SourceHit> toSources(List<SearchHit> hits) {
        List<SourceHit> sources = new ArrayList<>(hits.size());
        for (SearchHit h : hits) {
            sources.add(new SourceHit(h.getDocId(), h.getChunkIndex(), h.getContent()));
        }
        return sources;
    }

    /** LLM 偶发返回空白内容也算失败，兜底成固定提示。 */
    private String blankToFallback(String answer) {
        return (answer == null || answer.isBlank()) ? MODEL_UNAVAILABLE : answer;
    }
}