package com.selfcoach.plan;

/**
 * 单个训练动作（周计划里最小的一行）。
 * 对应出参 items 数组元素：{exercise, sets, reps}。
 */
public class PlanItem {

    /** 动作名，如「深蹲」「杠铃卧推」 */
    private final String exercise;

    /** 组数（数字） */
    private final int sets;

    /** 每组次数（字符串，因为常是区间，如 "8-12" / "3-5"） */
    private final String reps;

    public PlanItem(String exercise, int sets, String reps) {
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
    }

    public String getExercise() { return exercise; }
    public int getSets() { return sets; }
    public String getReps() { return reps; }
}