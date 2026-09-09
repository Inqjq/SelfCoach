package com.selfcoach.service;

import com.selfcoach.domain.TrainingLog;
import com.selfcoach.repository.TrainingLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 训练日志业务逻辑（F-01）。
 * Controller 只负责接请求，真正的操作在这里。
 */
@Service
public class TrainingLogService {

    private final TrainingLogRepository repo;

    public TrainingLogService(TrainingLogRepository repo) {
        this.repo = repo;
    }

    /** 新增一条训练记录。P0 单用户：没传 user 就默认给 1；组数没传默认 1。 */
    public Mono<TrainingLog> create(TrainingLog log) {
        if (log.getUserId() == null) log.setUserId(1L);
        if (log.getSets() == null) log.setSets(1);
        return repo.save(log);
    }

    /** 查询列表：支持按日期范围过滤；默认查用户 1。 */
    public Flux<TrainingLog> list(Long userId, LocalDate from, LocalDate to) {
        Long uid = userId == null ? 1L : userId;
        if (from != null && to != null) {
            return repo.findByUserIdAndWorkoutDateBetween(uid, from, to);
        }
        return repo.findByUserIdOrderByWorkoutDateDesc(uid);
    }

    /** 按 id 查单条 */
    public Mono<TrainingLog> get(Long id) {
        return repo.findById(id);
    }

    /** 更新：只覆盖传入的非空字段，其余保持原值（局部更新）。 */
    public Mono<TrainingLog> update(Long id, TrainingLog patch) {
        return repo.findById(id).flatMap(existing -> {
            if (patch.getWorkoutDate() != null) existing.setWorkoutDate(patch.getWorkoutDate());
            if (patch.getExercise() != null) existing.setExercise(patch.getExercise());
            existing.setWeightKg(patch.getWeightKg());
            if (patch.getReps() != null) existing.setReps(patch.getReps());
            if (patch.getSets() != null) existing.setSets(patch.getSets());
            return repo.save(existing);
        });
    }

    /** 删除 */
    public Mono<Void> delete(Long id) {
        return repo.deleteById(id);
    }
}