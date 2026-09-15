<template>
  <section class="view training">
    <header class="view-head">
      <h1>训练记录</h1>
      <p>汇总每一次的重量、次数与组数。</p>
    </header>

    <!-- 记忆点：本周已训练次数大数字 -->
    <div class="stat">
      <div class="stat-top">
        <span class="stat-label">本周已训练</span>
        <button type="button" class="btn" @click="openCreate">记录本次训练</button>
      </div>
      <div class="stat-value">
        <span class="num">{{ weekCount }}</span>
        <span class="stat-unit">次</span>
      </div>
      <div class="stat-note">本周一至今 · 累计 {{ logs.length }} 条记录</div>
    </div>

    <!-- 新增 / 编辑表单 -->
    <div v-if="formOpen" class="form-panel">
      <div class="form-title">{{ editingId ? '编辑训练记录' : '记录本次训练' }}</div>
      <form @submit.prevent="submit">
        <div class="form-grid">
          <label class="field">
            <span>训练日期</span>
            <input v-model="form.workoutDate" class="input" type="date" required />
          </label>
          <label class="field">
            <span>动作名称</span>
            <select v-model="form.exercise" class="input" required>
              <option disabled value="">选择动作…</option>
              <option v-if="form.exercise && !FLAT_EXERCISES.includes(form.exercise)" :value="form.exercise">
                {{ form.exercise }}
              </option>
              <optgroup v-for="g in EXERCISES" :key="g.group" :label="g.group">
                <option v-for="name in g.items" :key="name" :value="name">{{ name }}</option>
              </optgroup>
            </select>
          </label>
          <label class="field">
            <span>重量 (kg)</span>
            <input v-model="form.weightKg" class="input" type="number" min="0" step="0.5" placeholder="自重动作可留空" />
          </label>
          <label class="field">
            <span>次数</span>
            <input v-model="form.reps" class="input" type="number" min="1" step="1" placeholder="每组次数" />
          </label>
          <label class="field">
            <span>组数</span>
            <input v-model="form.sets" class="input" type="number" min="1" step="1" placeholder="默认 1" />
          </label>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn" :disabled="saving">{{ editingId ? '保存修改' : '保存' }}</button>
          <button type="button" class="btn ghost" @click="closeForm">取消</button>
        </div>
      </form>
    </div>

    <!-- 异常 -->
    <div v-if="error" class="error" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="error-close" @click="error = ''">收起</button>
    </div>

    <!-- 加载（仅首次，避免保存后刷新时列表闪烁） -->
    <p v-if="loading && !logs.length" class="hint">加载中…</p>

    <!-- 空状态 -->
    <div v-else-if="!logs.length" class="empty">
      <p class="kicker">从这里开始</p>
      <h2>还没有训练记录</h2>
      <p>点上方「记录本次训练」，记下你的第一次训练。</p>
    </div>

    <!-- 列表 -->
    <div v-else class="list">
      <div v-for="log in logs" :key="log.id" class="row">
        <div class="row-main">
          <div class="row-title">
            <span class="date">{{ log.workoutDate }}</span>
            <span class="ex">{{ log.exercise }}</span>
          </div>
          <div class="row-sub">{{ summary(log) }}</div>
        </div>
        <div class="row-actions">
          <template v-if="pendingDeleteId === log.id">
            <span class="confirm-text">确认删除？</span>
            <button type="button" class="btn small" @click="confirmDelete(log.id)">删除</button>
            <button type="button" class="btn ghost small" @click="pendingDeleteId = null">取消</button>
          </template>
          <template v-else>
            <button type="button" class="btn ghost small" @click="openEdit(log)">编辑</button>
            <button type="button" class="btn ghost small" @click="pendingDeleteId = log.id">删除</button>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'

// 常见动作下拉（按部位分组）；后端 exercise 是自由字符串，这里只在录入时提供快捷选择。
const EXERCISES = [
  { group: '腿部', items: ['深蹲', '杠铃深蹲', '保加利亚分腿蹲', '腿举', '硬拉', '罗马尼亚硬拉', '臀桥', '箭步蹲'] },
  { group: '胸部', items: ['杠铃卧推', '哑铃卧推', '上斜卧推', '俯卧撑', '双杠臂屈伸', '飞鸟'] },
  { group: '背部', items: ['引体向上', '高位下拉', '杠铃划船', '坐姿划船', '单臂哑铃划船'] },
  { group: '肩臂', items: ['肩推', '侧平举', '二头弯举', '三头下压'] },
  { group: '核心', items: ['卷腹', '平板支撑', '悬垂举腿'] },
  { group: '有氧 / 体能', items: ['跑步', '跳绳', '划船机', '波比跳'] }
]
const FLAT_EXERCISES = EXERCISES.flatMap((g) => g.items)

const logs = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const formOpen = ref(false)
const editingId = ref(null)
const pendingDeleteId = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return {
    workoutDate: todayLocal(),
    exercise: '',
    weightKg: '',
    reps: '',
    sets: ''
  }
}

// 本地日期 → "yyyy-MM-dd"，避免 toISOString 的时区偏移造成跨天误差。
function toDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function todayLocal() {
  return toDateStr(new Date())
}

// 本周（周一到今天）的训练次数：日期都是 yyyy-MM-dd 字符串，直接按字典序比较即可。
const weekCount = computed(() => {
  const now = new Date()
  const diff = (now.getDay() + 6) % 7 // 距离本周一的天数
  const monday = new Date(now)
  monday.setDate(now.getDate() - diff)
  return logs.value.filter(
    (l) => l.workoutDate >= toDateStr(monday) && l.workoutDate <= todayLocal()
  ).length
})

// 空字符串 → null；其余转数字。重量可为 null（自重动作），次数/组数可空走后端默认。
const toNum = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

async function fetchLogs() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.listTrainingLogs()
    logs.value = (data || []).slice().sort((a, b) => (a.workoutDate < b.workoutDate ? 1 : -1))
  } catch (e) {
    error.value = '加载训练记录失败，请确认后端可访问后重试。'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  editingId.value = null
  formOpen.value = true
}

function openEdit(log) {
  form.value = {
    workoutDate: log.workoutDate,
    exercise: log.exercise,
    weightKg: log.weightKg ?? '',
    reps: log.reps ?? '',
    sets: log.sets ?? ''
  }
  editingId.value = log.id
  formOpen.value = true
}

function closeForm() {
  formOpen.value = false
  editingId.value = null
}

async function submit() {
  const f = form.value
  if (!f.workoutDate || !f.exercise.trim()) {
    error.value = '请填写训练日期和动作名称。'
    return
  }
  const payload = {
    workoutDate: f.workoutDate,
    exercise: f.exercise.trim(),
    weightKg: toNum(f.weightKg),
    reps: toNum(f.reps),
    sets: toNum(f.sets)
  }

  saving.value = true
  error.value = ''
  try {
    if (editingId.value) {
      await api.updateTrainingLog(editingId.value, payload)
    } else {
      await api.createTrainingLog(payload)
    }
    closeForm()
    await fetchLogs()
  } catch (e) {
    error.value = '保存失败，请检查输入后重试。'
  } finally {
    saving.value = false
  }
}

async function confirmDelete(id) {
  error.value = ''
  try {
    await api.deleteTrainingLog(id)
    pendingDeleteId.value = null
    await fetchLogs()
  } catch (e) {
    error.value = '删除失败，请稍后重试。'
  }
}

function summary(log) {
  const parts = []
  if (log.weightKg != null) parts.push(`${log.weightKg} kg`)
  if (log.reps != null) parts.push(`${log.reps} 次`)
  if (log.sets != null) parts.push(`${log.sets} 组`)
  return parts.length ? parts.join(' × ') : '未填写重量 / 次数'
}

onMounted(fetchLogs)
</script>

<style scoped>
.training {
  max-width: 860px;
}

/* 列表 */
.list {
  border: 1px solid var(--line);
  border-radius: var(--radius);
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
}
.row + .row {
  border-top: 1px solid var(--line);
}
.row:hover {
  background: var(--panel);
}
.row-main {
  min-width: 0;
}
.row-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.date {
  font-size: 12px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.ex {
  font-weight: 600;
  font-size: 15px;
}
.row-sub {
  margin-top: 3px;
  font-size: 13px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.row-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
}
.confirm-text {
  font-size: 13px;
  color: var(--muted);
}
</style>