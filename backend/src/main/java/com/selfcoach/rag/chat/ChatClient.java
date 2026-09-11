package com.selfcoach.rag.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * LLM 对话客户端：调用阿里云百炼的 OpenAI 兼容 /chat/completions 接口做文本生成。
 * 风格与 {@code llm/EmbeddingClient} 完全对齐——用 WebClient 手写、不引 LLM 框架，
 * 复用同一个 base-url 和 api-key（都从 llm.* 配置读），只是换模型（chat-model）和端点。
 */
@Component
public class ChatClient {

    private final WebClient client;
    private final String model;

    public ChatClient(WebClient.Builder builder,
                      @Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.api-key}") String apiKey,
                      @Value("${llm.chat-model}") String model) {
        this.model = model;
        // 和 EmbeddingClient 一样：每个请求自动带 Authorization: Bearer <key>
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 一次对话：system + user 两条消息，返回 LLM 生成的文本。
     * 端点 /chat/completions，body {model, messages:[{role,content},...]}（OpenAI 兼容协议）。
     */
    public Mono<String> chat(String systemPrompt, String userPrompt) {
        return client.post()
                .uri("/chat/completions")
                .bodyValue(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt))))
                .retrieve()
                .bodyToMono(JsonNode.class)
                // 取第一个候选的 message.content；异常（非 2xx / 解析失败）会以 onError 抛出，由上层降级
                .map(node -> node.path("choices").path(0).path("message").path("content").asText());
    }
}