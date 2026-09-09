package com.selfcoach.repository;

import com.selfcoach.domain.BodyMetric;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 身体指标的响应式数据访问层（F-02）。
 * 继承 ReactiveCrudRepository 拿到基础增删改查（save/findById/findAll/delete 等），
 * 其余按业务补方法，Spring Data R2DBC 按方法名自动生成 SQL。
 */
public interface BodyMetricRepository extends ReactiveCrudRepository<BodyMetric, Long> {

    /**
     * 查「某用户某一天」的指标，用于记录时判断该天是否已存在（存在→覆盖，不存在→新增）。
     * 因为表有 UNIQUE(user_id, measure_date)，至多一条，返回 Mono。
     */
    Mono<BodyMetric> findByUserIdAndMeasureDate(Long userId, LocalDate measureDate);

    /**
     * 按日期范围查趋势，日期升序（from → to），画趋势图要按时间轴排。
     * 注意与 F-01 的 Desc 相反：趋势图要时间正序，所以这里是 OrderByMeasureDateAsc。
     */
    Flux<BodyMetric> findByUserIdAndMeasureDateBetweenOrderByMeasureDateAsc(
            Long userId, LocalDate from, LocalDate to);

    /**
     * 查某用户全部指标、按日期升序（最早在前）。
     * 供 trend 在「没传 from/to」时退化为查全量趋势，方向与范围查询保持一致（都是 Asc）。
     */
    Flux<BodyMetric> findByUserIdOrderByMeasureDateAsc(Long userId);

    /**
     * 查某用户全部指标、按日期倒序（最新在前）。
     * 「最近一次」= 取这条 Flux 的第一条，由 Service 用 .next() 完成。
     */
    Flux<BodyMetric> findByUserIdOrderByMeasureDateDesc(Long userId);
}