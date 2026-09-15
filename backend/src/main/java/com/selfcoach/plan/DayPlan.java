package com.selfcoach.plan;

import java.util.List;

/**
 * 一天的训练安排。
 * 对应出参 plan 数组元素：{day, focus, items}。
 */
public class DayPlan {

    /** 星期几，如「周一」 */
    private final String day;

    /** 当天训练部位/重点，如「下肢」「上肢」「推」「拉」「腿」「全身」 */
    private final String focus;

    /** 当天动作列表（含热身/主项/辅助） */
    private final List<PlanItem> items;

    public DayPlan(String day, String focus, List<PlanItem> items) {
        this.day = day;
        this.focus = focus;
        this.items = items;
    }

    public String getDay() { return day; }
    public String getFocus() { return focus; }
    public List<PlanItem> getItems() { return items; }
}