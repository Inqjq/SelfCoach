package com.selfcoach.api;

import com.selfcoach.domain.BodyMetric;
import com.selfcoach.service.BodyMetricService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 身体指标 REST 接口（F-02）：
 *   POST /api/body-metrics          记录（或覆盖）当天指标
 *   GET  /api/body-metrics?from=&to= 按日期范围查趋势（升序）
 *   GET  /api/body-metrics/latest   取最近一次
 * 约定：P0 单用户，未指定 userId 时默认查/写用户 1。
 */
@RestController
@RequestMapping("/api/body-metrics")
public class BodyMetricController {

    private final BodyMetricService service;

    public BodyMetricController(BodyMetricService service) {
        this.service = service;
    }

    /**
     * 记录（或覆盖）当天指标。
     * 返回 200（默认）而非 201：因为它既可能是新增、也可能是覆盖，语义上是 upsert，不是纯创建。
     */
    @PostMapping
    public Mono<BodyMetric> record(@RequestBody BodyMetric bodyMetric) {
        return service.record(bodyMetric);
    }

    /**
     * 按日期范围查趋势（升序）。
     * 例如 GET /api/body-metrics?from=2026-09-01&to=2026-09-07
     * from/to 用 @DateTimeFormat(ISO.DATE) 解析成 LocalDate（"yyyy-MM-dd"）。
     */
    @GetMapping
    public Flux<BodyMetric> trend(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.trend(userId, from, to);
    }

    /**
     * 取最近一次记录。
     * 注意：本类没有 /{id} 路由，所以 /latest 不会被路径变量吞掉；
     * 将来若加 /{id}，/latest 必须声明在其之前（Spring 按字面匹配优先，写前面更保险）。
     */
    @GetMapping("/latest")
    public Mono<BodyMetric> latest(@RequestParam(required = false) Long userId) {
        return service.latest(userId);
    }
}