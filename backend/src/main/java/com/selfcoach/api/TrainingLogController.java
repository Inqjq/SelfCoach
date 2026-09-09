package com.selfcoach.api;

import com.selfcoach.domain.TrainingLog;
import com.selfcoach.service.TrainingLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 训练日志 REST 接口（F-01）：增删改查。
 * 约定：P0 单用户，未指定 userId 时默认查/写用户 1。
 */
@RestController
@RequestMapping("/api/training-logs")
public class TrainingLogController {

    private final TrainingLogService service;

    public TrainingLogController(TrainingLogService service) {
        this.service = service;
    }

    /** 新增一条训练记录 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TrainingLog> create(@RequestBody TrainingLog log) {
        return service.create(log);
    }

    /** 查询列表：可过滤日期范围，例如 /api/training-logs?from=2026-09-01&to=2026-09-07 */
    @GetMapping
    public Flux<TrainingLog> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(userId, from, to);
    }

    /** 查单条 */
    @GetMapping("/{id}")
    public Mono<TrainingLog> get(@PathVariable Long id) {
        return service.get(id);
    }

    /** 更新（局部更新） */
    @PutMapping("/{id}")
    public Mono<TrainingLog> update(@PathVariable Long id, @RequestBody TrainingLog patch) {
        return service.update(id, patch);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}