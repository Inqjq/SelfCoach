package com.selfcoach.plan;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 训练计划生成器（F-04）：用「规则 + 模板」确定性地生成周计划，不依赖 LLM，每一步都可解释。
 *
 * <p>为什么不用 LLM 生成：计划生成是「结构性、可枚举」的活——同一个目标/频率的合理计划
 * 有标准范式（分化方式 + 动作库 + 组次区间），用规则生成既确定性（多次调用结果一致、易测试）、
 * 又省 LLM 调用成本与延迟；LLM 更适合用在「理解用户意图/个性化」这类非结构化场景，后续可叠加。</p>
 *
 * <p>三层规则：</p>
 * <ol>
 *   <li><b>分化模板</b>：daysPerWeek 1-3 → 全身；4 → 上下肢；5-6 → 推拉腿（PPL）。</li>
 *   <li><b>动作库</b>：每个 focus（部位/重点）配 6 个动作 = 1 热身 + 2 主项 + 3 辅助，
 *       每天按 focus 取动作，保证「多关节大动作打头、单关节辅助收尾」的合理顺序。</li>
 *   <li><b>组次</b>：按「动作角色 × goal」给 sets/reps——增肌中高容量(主项 6-12)、
 *       力量高强度低次(主项 3-5)、减脂中等容量保留大重量主项(主项 8-12)。</li>
 * </ol>
 */
@Service
public class PlanGenerator {

    /** 星期中文名，训练日从周一开始连续排 */
    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /** 动作角色：决定组次规则怎么给 */
    private enum Role { WARMUP, PRIMARY, ACCESSORY }

    /** 动作库条目：动作名 + 角色 */
    private record ExerciseDef(String name, Role role) {}

    /** 动作库：focus → 该部位的动作清单（顺序即训练顺序） */
    private static final Map<String, List<ExerciseDef>> LIBRARY = buildLibrary();

    private static Map<String, List<ExerciseDef>> buildLibrary() {
        Map<String, List<ExerciseDef>> lib = new LinkedHashMap<>();

        // 全身日：下肢+推+拉+核心各覆盖一次，只有大项没有细分辅助
        lib.put("全身", List.of(
                new ExerciseDef("动态热身", Role.WARMUP),
                new ExerciseDef("深蹲", Role.PRIMARY),
                new ExerciseDef("杠铃卧推", Role.PRIMARY),
                new ExerciseDef("杠铃划船", Role.ACCESSORY),
                new ExerciseDef("哑铃推举", Role.ACCESSORY),
                new ExerciseDef("平板支撑", Role.ACCESSORY)));

        // 腿日（4 天分化的「下肢」和 PPL 的「腿」共用这一套）
        List<ExerciseDef> legDay = List.of(
                new ExerciseDef("动态热身", Role.WARMUP),
                new ExerciseDef("深蹲", Role.PRIMARY),
                new ExerciseDef("罗马尼亚硬拉", Role.PRIMARY),
                new ExerciseDef("腿举", Role.ACCESSORY),
                new ExerciseDef("腿弯举", Role.ACCESSORY),
                new ExerciseDef("站姿提踵", Role.ACCESSORY));
        lib.put("下肢", legDay);
        lib.put("腿", legDay);

        // 上肢日：胸(卧推)+背(引体)为主项，肩/背/二头做辅助
        lib.put("上肢", List.of(
                new ExerciseDef("动态热身", Role.WARMUP),
                new ExerciseDef("杠铃卧推", Role.PRIMARY),
                new ExerciseDef("引体向上", Role.PRIMARY),
                new ExerciseDef("坐姿哑铃推举", Role.ACCESSORY),
                new ExerciseDef("坐姿划船", Role.ACCESSORY),
                new ExerciseDef("哑铃弯举", Role.ACCESSORY)));

        // 推日：胸+肩+三头
        lib.put("推", List.of(
                new ExerciseDef("动态热身", Role.WARMUP),
                new ExerciseDef("杠铃卧推", Role.PRIMARY),
                new ExerciseDef("站姿杠铃推举", Role.PRIMARY),
                new ExerciseDef("上斜哑铃卧推", Role.ACCESSORY),
                new ExerciseDef("哑铃侧平举", Role.ACCESSORY),
                new ExerciseDef("绳索三头下压", Role.ACCESSORY)));

        // 拉日：背+二头
        lib.put("拉", List.of(
                new ExerciseDef("动态热身", Role.WARMUP),
                new ExerciseDef("传统硬拉", Role.PRIMARY),
                new ExerciseDef("引体向上", Role.PRIMARY),
                new ExerciseDef("高位下拉", Role.ACCESSORY),
                new ExerciseDef("面拉", Role.ACCESSORY),
                new ExerciseDef("杠铃弯举", Role.ACCESSORY)));

        return lib;
    }

    /** 入口：goal(增肌/力量/减脂) + daysPerWeek → 结构化周计划 */
    public PlanResponse generate(String goal, int daysPerWeek) {
        String g = normalizeGoal(goal);
        List<String> focusSeq = focusSequence(daysPerWeek);

        List<DayPlan> days = new ArrayList<>(focusSeq.size());
        for (int i = 0; i < focusSeq.size(); i++) {
            days.add(buildDay(WEEKDAYS[i], focusSeq.get(i), g));
        }
        return new PlanResponse(days);
    }

    /** 按 focus 生成一天：取该部位动作清单，逐项按 goal 配组次。 */
    private DayPlan buildDay(String day, String focus, String goal) {
        List<ExerciseDef> defs = LIBRARY.get(focus);
        List<PlanItem> items = new ArrayList<>(defs.size());
        for (ExerciseDef def : defs) {
            items.add(new PlanItem(def.name(), sets(def.role(), goal), reps(def.role(), goal)));
        }
        return new DayPlan(day, focus, items);
    }

    /** 组数：热身固定 2 组；主项「力量日升到 5 组、其余 4 组」；辅助统一 3 组。 */
    private int sets(Role role, String goal) {
        return switch (role) {
            case WARMUP -> 2;
            case PRIMARY -> "力量".equals(goal) ? 5 : 4;
            case ACCESSORY -> 3;
        };
    }

    /** 次数：热身不随 goal 变；主项/辅助按 goal 的强度区间给。 */
    private String reps(Role role, String goal) {
        if (role == Role.WARMUP) {
            return "12-15";
        }
        if (role == Role.PRIMARY) {
            return switch (goal) {
                case "力量" -> "3-5";   // 高强度低次
                case "减脂" -> "8-12";  // 中等容量、保留大重量
                default -> "6-12";      // 增肌：中高容量
            };
        }
        // 辅助动作：容量更高、更孤立，次数整体比主项高一个档
        return switch (goal) {
            case "力量" -> "6-8";
            case "减脂" -> "12-15";
            default -> "10-15"; // 增肌
        };
    }

    /** 分化模板：按每周天数给「每天练什么」的顺序。 */
    private List<String> focusSequence(int daysPerWeek) {
        int d = Math.max(1, Math.min(6, daysPerWeek)); // 收敛到 1..6（任务定义的上限是 6）
        if (d <= 3) {
            return Collections.nCopies(d, "全身");      // 1-3 天：天天全身
        }
        if (d == 4) {
            return List.of("下肢", "上肢", "下肢", "上肢"); // 4 天：上下肢分化
        }
        if (d == 5) {
            return List.of("推", "拉", "腿", "推", "拉");  // 5 天：推拉腿+推拉
        }
        return List.of("推", "拉", "腿", "推", "拉", "腿"); // 6 天：完整推拉腿
    }

    /** goal 归一化：只认「力量/减脂」，其余（含增肌、空、乱填）一律落「增肌」。 */
    private String normalizeGoal(String goal) {
        String g = goal == null ? "" : goal.trim();
        return switch (g) {
            case "力量", "减脂" -> g;
            default -> "增肌";
        };
    }
}