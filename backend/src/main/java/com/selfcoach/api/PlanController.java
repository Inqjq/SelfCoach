package com.selfcoach.api;

import com.selfcoach.plan.PlanGenerator;
import com.selfcoach.plan.PlanResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 训练计划接口（F-04）：
 * POST /api/plan/generate  body: {"goal":"增肌|减脂|力量","daysPerWeek":4}
 * 返回 {plan:[{day,focus,items:[{exercise,sets,reps}]}]}，纯规则生成，不落库。
 */
@RestController
public class PlanController {

    /** daysPerWeek 未传或非法时的兜底：每周 3 天（全身，最保守的默认） */
    private static final int DEFAULT_DAYS = 3;

    private final PlanGenerator planGenerator;

    public PlanController(PlanGenerator planGenerator) {
        this.planGenerator = planGenerator;
    }

    /**
     * 生成周计划。入参用 Map 接收（与 Ingest/Chat 控制器风格一致），
     * 手动解析 goal 与 daysPerWeek——这里不引 DTO，因为就两个字段、解析逻辑一眼能看完。
     */
    @PostMapping("/api/plan/generate")
    public Mono<PlanResponse> generate(@RequestBody Map<String, Object> body) {
        String goal = body.get("goal") == null ? "" : String.valueOf(body.get("goal"));
        int daysPerWeek = parseDays(body.get("daysPerWeek"));
        // 规则生成是纯 CPU、无 IO；用 Mono.just 包裹只是为了和全站响应式返回类型保持一致
        return Mono.just(planGenerator.generate(goal, daysPerWeek));
    }

    /** 把入参里的 daysPerWeek 安全转成 int：数字直接用，字符串尝试解析，其余用默认值。 */
    private int parseDays(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                // 落到默认值
            }
        }
        return DEFAULT_DAYS;
    }
}