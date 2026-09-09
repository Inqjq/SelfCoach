package com.selfcoach.service;

import com.selfcoach.domain.BodyMetric;
import com.selfcoach.repository.BodyMetricRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 身体指标业务逻辑（F-02）。
 * Controller 只负责接请求，真正的操作在这里。
 */
@Service
public class BodyMetricService {

    private final BodyMetricRepository repo;

    public BodyMetricService(BodyMetricRepository repo) {
        this.repo = repo;
    }

    /**
     * 记录（或覆盖）某天的身体指标。
     * 语义：同一天再记录 = 字段级合并——请求里非 null 的字段覆盖旧值，null 的字段保留原值。
     * 理由：真实场景是「每天称体重、每周量围度」，全量覆盖会误清空没量到的围度。
     */
    public Mono<BodyMetric> record(BodyMetric m) {
        // P0 单用户：没传 userId 默认 1；没传日期默认今天（「记录当天」）。
        if (m.getUserId() == null) m.setUserId(1L);
        if (m.getMeasureDate() == null) m.setMeasureDate(LocalDate.now());

        Long uid = m.getUserId();
        LocalDate date = m.getMeasureDate();

        // 先查后写：先看这一天是否已有一条（表有 UNIQUE(user_id, measure_date)，至多一条）。
        return repo.findByUserIdAndMeasureDate(uid, date)
                .flatMap(existing -> {
                    // 已存在 → 只覆盖请求里非 null 的字段，其余（含 id/userId/measureDate/createdAt）保持不动。
                    if (m.getWeightKg() != null) existing.setWeightKg(m.getWeightKg());
                    if (m.getChestCm() != null) existing.setChestCm(m.getChestCm());
                    if (m.getWaistCm() != null) existing.setWaistCm(m.getWaistCm());
                    if (m.getHipCm() != null) existing.setHipCm(m.getHipCm());
                    if (m.getArmCm() != null) existing.setArmCm(m.getArmCm());
                    // 绝不动 createdAt：它记录的是「这条数据首次落库」的时刻，覆盖不应改写它。
                    return repo.save(existing);
                })
                // 这一天还没有 → 新增一条（id 为 null，DB 自增 + 补 created_at）。
                .switchIfEmpty(repo.save(m));
    }

    /**
     * 查询趋势：按日期升序返回（画趋势图要时间正序）。
     * 传了 from/to 就走范围；都没传就退化为查全量（同样升序）。
     */
    public Flux<BodyMetric> trend(Long userId, LocalDate from, LocalDate to) {
        Long uid = userId == null ? 1L : userId;
        if (from != null && to != null) {
            return repo.findByUserIdAndMeasureDateBetweenOrderByMeasureDateAsc(uid, from, to);
        }
        return repo.findByUserIdOrderByMeasureDateAsc(uid);
    }

    /**
     * 取最近一次记录：先按日期倒序排（最新在前），再取第一条。
     * .next() 把 Flux 折叠成 Mono（空表则返回空的 Mono，不是报错）。
     */
    public Mono<BodyMetric> latest(Long userId) {
        Long uid = userId == null ? 1L : userId;
        return repo.findByUserIdOrderByMeasureDateDesc(uid).next();
    }
}