package com.selfcoach.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动自检（仅用于验证 Embedding 接入是否打通）。
 * 若配了 BAILIAN_API_KEY，则启动时把「深蹲要领」转成向量并打印维度，
 * 让我在云服务器日志里确认「接口能通 + 维度=1024」。key 为空则静默跳过，不影响正常启动。
 */
@Component
public class EmbeddingCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingCheck.class);

    private final EmbeddingClient embedClient;
    private final String apiKey;

    public EmbeddingCheck(EmbeddingClient embedClient, @Value("${llm.api-key}") String apiKey) {
        this.embedClient = embedClient;
        this.apiKey = apiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("未配置 BAILIAN_API_KEY，跳过 Embedding 自检");
            return;
        }
        embedClient.embed("深蹲要领")
                .subscribe(
                        v -> log.info("Embedding 接入 OK —— 维度 {}（向量前5维 {}）", v.length, preview(v)),
                        err -> log.error("Embedding 接入失败：{}", err.getMessage(), err)
                );
    }

    private String preview(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(5, v.length); i++) sb.append(String.format("%.4f ", v[i]));
        return sb.append("]").toString();
    }
}