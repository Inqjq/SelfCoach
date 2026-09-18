<template>
  <section class="view plan">
    <header class="view-head">
      <h1>训练计划</h1>
      <p>选目标和每周能练几天，生成一份可执行的分化计划。</p>
    </header>

    <!-- 输入面板 -->
    <div class="config">
      <div class="config-row">
        <span class="config-label">目标</span>
        <div class="seg">
          <button
            v-for="g in GOALS"
            :key="g.value"
            type="button"
            class="seg-item"
            :class="{ on: goal === g.value }"
            :aria-pressed="goal === g.value"
            @click="goal = g.value"
          >
            <span class="seg-name">{{ g.label }}</span>
            <span class="seg-hint">{{ g.hint }}</span>
          </button>
        </div>
      </div>

      <div class="config-row">
        <span class="config-label">我的可用天数</span>
        <div class="days">
          <button
            v-for="(d, i) in WEEKDAYS"
            :key="d"
            type="button"
            class="day-chip"
            :class="{ on: selected[i] }"
            :aria-pressed="selected[i]"
            :disabled="!selected[i] && daysCount >= 6"
            @click="toggleDay(i)"
          >
            {{ d }}
          </button>
        </div>
      </div>

      <p class="config-note">
        <template v-if="daysCount">已选 {{ daysCount }} 天</template>
        <template v-else>先点亮你能训练的日子</template>
        <span v-if="daysCount >= 6"> · 每周最多 6 天</span>
      </p>

      <button type="button" class="btn" :disabled="!daysCount || loading" @click="generate">
        {{ plan.length ? '重新生成' : '生成计划' }}
      </button>
    </div>

    <!-- 异常 -->
    <div v-if="error" class="error" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="error-close" @click="error = ''">收起</button>
    </div>

    <!-- 生成中 -->
    <p v-if="loading" class="hint">生成中…</p>

    <!-- 周计划 -->
    <div v-else-if="plan.length" class="plan-days">
      <div v-for="d in plan" :key="d.day" class="plan-day">
        <div class="plan-day-head">
          <span class="plan-day-name">{{ d.day }}</span>
          <span class="plan-day-focus">{{ d.focus }}</span>
        </div>
        <ul class="plan-items">
          <li v-for="(it, j) in d.items" :key="j" class="plan-item">
            <span class="plan-ex">{{ it.exercise }}</span>
            <span class="plan-sr">{{ it.sets }} 组 × {{ it.reps }} 次</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty">
      <p class="kicker">从这里开始</p>
      <h2>还没有训练计划</h2>
      <p>选好目标和每周能练几天，点「生成计划」。</p>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'
import { api } from '../api'

const GOALS = [
  { value: '增肌', label: '增肌', hint: '增围度' },
  { value: '减脂', label: '减脂', hint: '保力量' },
  { value: '力量', label: '力量', hint: '冲重量' }
]
const WEEKDAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const goal = ref('增肌')
const selected = ref([true, false, true, false, true, false, false]) // 默认一三五
const plan = ref([])
const loading = ref(false)
const error = ref('')

const daysCount = computed(() => selected.value.filter(Boolean).length)
const selectedDayNames = computed(() => WEEKDAYS.filter((_, i) => selected.value[i]))

function toggleDay(i) {
  if (!selected.value[i] && daysCount.value >= 6) return // 最多 6 天，后端也只收敛到 6
  selected.value[i] = !selected.value[i]
}

async function generate() {
  if (!daysCount.value || loading.value) return
  loading.value = true
  error.value = ''
  try {
    const data = await api.generatePlan({ goal: goal.value, daysPerWeek: daysCount.value })
    const days = (data && data.plan) || []
    // 后端返回的 day 是「周一…」连续标签，内容按顺序生成；
    // 这里把标签重映射成用户实际点选的训练日，让计划读起来是「我的这一天」。
    const names = selectedDayNames.value
    plan.value = days.map((d, i) => ({ ...d, day: names[i] || d.day }))
  } catch (e) {
    error.value = '生成计划失败，请确认后端可访问后重试。'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.plan {
  max-width: 900px;
}

.config {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px 22px;
  margin-bottom: 20px;
}
.config-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;
}
.config-label {
  font-size: 13px;
  color: var(--muted);
}

.seg {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 10px;
}
.seg-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 12px 14px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  color: var(--text);
  cursor: pointer;
  text-align: left;
}
.seg-item.on {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--accent-ink);
}
.seg-name {
  font-weight: 700;
  font-size: 14.5px;
}
.seg-hint {
  font-size: 12px;
  color: var(--muted);
}
.seg-item.on .seg-hint {
  color: rgba(20, 23, 25, 0.72);
}

.days {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.day-chip {
  padding: 8px 14px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  color: var(--muted);
  cursor: pointer;
  font-size: 13.5px;
}
.day-chip.on {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--accent-ink);
  font-weight: 700;
}
.day-chip:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.config-note {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--muted);
}

.plan-days {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}
.plan-day {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}
.plan-day-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}
.plan-day-name {
  font-weight: 700;
  font-size: 15px;
}
.plan-day-focus {
  font-size: 12px;
  color: var(--accent);
  padding: 1px 8px;
  border: 1px solid var(--accent);
  border-radius: 3px;
}
.plan-items {
  list-style: none;
  margin: 0;
  padding: 6px 0;
}
.plan-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  font-size: 14px;
}
.plan-item + .plan-item {
  border-top: 1px solid var(--line);
}
.plan-ex {
  color: var(--text);
}
.plan-sr {
  color: var(--muted);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>