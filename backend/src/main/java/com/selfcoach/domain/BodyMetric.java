package com.selfcoach.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 身体指标实体，对应 body_metric 表（F-02）。
 * 每天每个用户只存一条（表上有 UNIQUE(user_id, measure_date)），
 * 同一 measure_date 再次记录走「覆盖/更新」逻辑，在 Service 层实现。
 * 字段命名与 TrainingLog 一致：Java 用 camelCase、DB 列用 snake_case，
 * 用 @Column 显式映射，一眼能对上「实体字段 ↔ 数据库列」。
 */
@Table("body_metric")
public class BodyMetric {

    @Id
    private Long id;

    /** 所属用户（P0 单用户，默认 1） */
    @Column("user_id")
    private Long userId;

    /** 测量日期（每天一条的关键字段） */
    @Column("measure_date")
    private LocalDate measureDate;

    /** 体重(kg)，NUMERIC(5,2)，可空（当天没称就 null） */
    @Column("weight_kg")
    private BigDecimal weightKg;

    /** 胸围(cm)，NUMERIC(5,1)，可空 */
    @Column("chest_cm")
    private BigDecimal chestCm;

    /** 腰围(cm)，NUMERIC(5,1)，可空 */
    @Column("waist_cm")
    private BigDecimal waistCm;

    /** 臀围(cm)，NUMERIC(5,1)，可空 */
    @Column("hip_cm")
    private BigDecimal hipCm;

    /** 臂围(cm)，NUMERIC(5,1)，可空 */
    @Column("arm_cm")
    private BigDecimal armCm;

    /** 创建时间，DB 默认 now() 生成，插入后回读 */
    @Column("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getMeasureDate() { return measureDate; }
    public void setMeasureDate(LocalDate measureDate) { this.measureDate = measureDate; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public BigDecimal getChestCm() { return chestCm; }
    public void setChestCm(BigDecimal chestCm) { this.chestCm = chestCm; }

    public BigDecimal getWaistCm() { return waistCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }

    public BigDecimal getHipCm() { return hipCm; }
    public void setHipCm(BigDecimal hipCm) { this.hipCm = hipCm; }

    public BigDecimal getArmCm() { return armCm; }
    public void setArmCm(BigDecimal armCm) { this.armCm = armCm; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}