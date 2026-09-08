package com.selfcoach.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 健康检查接口，用于验证骨架能正常启动、前后端能连通。
 * 属于基础设施，不属于业务功能。
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "ok", "service", "selfcoach-backend"));
    }
}