package com.selfcoach.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Embedding 客户端：调用阿里云百炼的 OpenAI 兼容接口做文本向量化（RAG 入库/检索都要用）。
 * 端点：POST {base-url}/embeddings，body {model, input, dimensions}。
 *
 * <p>为什么自己用 WebClient 手写而不是引 LLM 框架：项目要求「每行都能讲清 + 核心逻辑手写」，
 * 百炼是 OpenAI 兼容协议，一个简单的 POST 就能拿到向量，引入框架反而是黑盒。</p>
 */
@Component
public class EmbeddingClient {

    /** 输出维度：必须与我们建表时的 doc_chunk.embedding vector(1024) 对齐 */
    private static final int DIMENSIONS = 1024;

    private final WebClient client;
    private final String model;

    public EmbeddingClient(WebClient.Builder builder,
                           @Value("${llm.base-url}") String baseUrl,
                           @Value("${llm.api-key}") String apiKey,
                           @Value("${llm.embedding-model}") String model) {
        this.model = model;
        // 每个请求自动带上 Authorization: Bearer <key>
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 把一段文本转成 1024 维 float 向量。
     * 响应是 {data:[{embedding:[...]}], usage:{...}}，这里只取第一条 embedding。
     */
    public Mono<float[]> embed(String text) {
        return client.post()
                .uri("/embeddings")
                .bodyValue(Map.of(
                        "model", model,
                        "input", List.of(text),
                        "dimensions", DIMENSIONS))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> toFloats(node.path("data").path(0).path("embedding")));
    }

    private float[] toFloats(JsonNode arr) {
        int n = arr.size();
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            out[i] = arr.get(i).floatValue();
        }
        return out;
    }
}