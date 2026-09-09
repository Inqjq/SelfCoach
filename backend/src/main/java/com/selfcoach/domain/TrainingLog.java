package com.selfcoach.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 训练日志实体，对应 training_log 表（F-01）。
 * 字段名用 camelCase，DB 列用 snake_case，通过 @Column 显式映射，
 * 这样一眼能看出 Java 字段与数据库列的对应关系。
 */
@Table("training_log")
public class TrainingLog {

    @Id
    private Long id;

    /** 所属用户（P0 单用户，默认 1） */
    @Column("user_id")
    private Long userId;

    /** 训练日期 */
    @Column("workout_date")
    private LocalDate workoutDate;

    /** 动作名，如「杠铃卧推」 */
    private String exercise;

    /** 重量(kg)，自重动作可为 null */
    @Column("weight_kg")
    private BigDecimal weightKg;

    /** 每组次数 */
    private Integer reps;

    /** 组数，默认 1 */
    private Integer sets;

    @Column("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDate workoutDate) { this.workoutDate = workoutDate; }

    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}