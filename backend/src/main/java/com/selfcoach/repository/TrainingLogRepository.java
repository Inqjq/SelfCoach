package com.selfcoach.repository;

import com.selfcoach.domain.TrainingLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * 训练日志的响应式数据访问层。
 * ReactiveCrudRepository 内置了基础的增删改查（save/findById/findAll/delete 等），
 * 这里只需补充按业务需要的查询方法，Spring Data R2DBC 会按方法名自动生成 SQL。
 */
public interface TrainingLogRepository extends ReactiveCrudRepository<TrainingLog, Long> {

    /** 查某个用户最近的训练（按日期倒序，最近的在前） */
    Flux<TrainingLog> findByUserIdOrderByWorkoutDateDesc(Long userId);

    /** 按日期范围查某个用户的训练 */
    Flux<TrainingLog> findByUserIdAndWorkoutDateBetween(Long userId, LocalDate from, LocalDate to);
}