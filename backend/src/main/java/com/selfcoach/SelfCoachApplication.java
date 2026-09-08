package com.selfcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SelfCoach 后端启动类。
 *
 * <p>骨架阶段只负责把服务跑起来，业务功能（RAG / Agent / 记忆等）
 * 按 {@code docs/开发计划.md} 的 Phase 逐步加入。</p>
 */
@SpringBootApplication
public class SelfCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelfCoachApplication.class, args);
    }
}