package com.selfcoach.plan;

import java.util.List;

/**
 * 训练计划接口 POST /api/plan/generate 的响应体：
 *   plan —— 整周安排（每个元素是一天 DayPlan）
 */
public class PlanResponse {

    private final List<DayPlan> plan;

    public PlanResponse(List<DayPlan> plan) {
        this.plan = plan;
    }

    public List<DayPlan> getPlan() { return plan; }
}