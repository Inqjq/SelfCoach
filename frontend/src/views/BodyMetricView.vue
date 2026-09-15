<template>
  <section class="view body">
    <header class="view-head">
      <h1>身体指标</h1>
      <p>记录体重与围度，看长期趋势。</p>
    </header>

    <!-- 最新体重 + 记录入口 -->
    <div class="stat">
      <div class="stat-top">
        <span class="stat-label">最新体重</span>
        <button type="button" class="btn" @click="openForm">{{ formOpen ? '收起' : '记录当天指标' }}</button>
      </div>
      <template v-if="latestWeight">
        <div class="stat-value">
          <span class="num">{{ latestWeight.w }}</span>
          <span class="stat-unit">kg</span>
        </div>
        <div class="stat-note">{{ latestWeight.date }}</div>
      </template>
      <p v-else class="stat-empty">还没记录体重</p>
    </div>

    <!-- 记录表单 -->
    <div v-if="formOpen" class="form-panel">
      <div class="form-title">记录当天指标</div>
      <form @submit.prevent="submit">
        <div class="form-grid">
          <label class="field">
            <span>测量日期</span>
            <input v-model="form.measureDate" class="input" type="date" required />
          </label>
          <label class="field">
            <span>体重 (kg)</span>
            <input v-model="form.weightKg" class="input" type="number" min="0" step="0.1" placeholder="例如 72.5" required />
          </label>
          <label class="field">
            <span>胸围 (cm)</span>
            <input v-model="form.chestCm" class="input" type="number" min="0" step="0.5" />
          </label>
          <label class="field">
            <span>腰围 (cm)</span>
            <input v-model="form.waistCm" class="input" type="number" min="0" step="0.5" />
          </label>
          <label class="field">
            <span>臀围 (cm)</span>
            <input v-model="form.hipCm" class="input" type="number" min="0" step="0.5" />
          </label>
          <label class="field">
            <span>臂围 (cm)</span>
            <input v-model="form.armCm" class="input" type="number" min="0" step="0.5" />
          </label>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn" :disabled="saving">记录</button>
          <button type="button" class="btn ghost" @click="closeForm">取消</button>
        </div>
      </form>
    </div>

    <!-- 异常 -->
    <div v-if="error" class="error" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="error-close" @click="error = ''">收起</button>
    </div>

    <!-- 加载 -->
    <p v-if="loading" class="hint">加载中…</p>

    <!-- 图表 / 空状态 -->
    <template v-else>
      <div v-if="chart" class="chart-panel">
        <div class="chart-head">
          <span class="chart-title">体重趋势</span>
          <span class="chart-sub">{{ chart.count }} 次记录 · kg</span>
        </div>
        <svg :viewBox="`0 0 ${chart.W} ${chart.H}`" class="chart" role="img" aria-label="体重趋势折线图">
          <g v-for="g in chart.grid" :key="g.label">
            <line :x1="chart.padL" :y1="g.y" :x2="chart.right" :y2="g.y" class="grid-line" />
            <text :x="chart.padL - 8" :y="g.y + 4" class="tick" text-anchor="end">{{ g.label }}</text>
          </g>
          <polyline :points="chart.line" class="line" fill="none" />
          <circle v-for="(p, i) in chart.points" :key="i" :cx="p.x" :cy="p.y" r="3.2" class="dot" />
          <text :x="chart.padL" :y="chart.bottom + 16" class="tick">{{ chart.firstLabel }}</text>
          <text :x="chart.right" :y="chart.bottom + 16" class="tick" text-anchor="end">{{ chart.lastLabel }}</text>
        </svg>
      </div>

      <div v-else-if="metrics.length" class="empty">
        <p class="kicker">再多记几次</p>
        <h2>趋势线需要至少两个体重点</h2>
        <p>再记录一天体重，这里就会画出你的体重趋势线。</p>
      </div>

      <div v-else class="empty">
        <p class="kicker">从这里开始</p>
        <h2>还没有身体指标</h2>
        <p>点上方「记录当天指标」，记下今天的体重。</p>
      </div>
    </template>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'

const metrics = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const formOpen = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { measureDate: todayLocal(), weightKg: '', chestCm: '', waistCm: '', hipCm: '', armCm: '' }
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

const toNum = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

// 只取有体重的记录，按日期升序（后端本身升序，这里再保证一次）。
const weightPoints = computed(() =>
  metrics.value
    .filter((m) => m.weightKg != null)
    .map((m) => ({
      date: m.measureDate,
      w: Number(m.weightKg),
      t: new Date(`${m.measureDate}T00:00:00`).getTime()
    }))
)
const weightCount = computed(() => weightPoints.value.length)
const latestWeight = computed(() =>
  weightPoints.value.length ? weightPoints.value[weightPoints.value.length - 1] : null
)

function round1(n) {
  return Math.round(n * 10) / 10
}

// 手写 SVG 折线图的坐标计算：x 按真实日期间距、y 按体重线性映射。
const chart = computed(() => {
  const pts = weightPoints.value
  if (pts.length < 2) return null

  const W = 680
  const H = 240
  const padL = 48
  const padR = 20
  const padT = 18
  const padB = 30
  const innerW = W - padL - padR
  const innerH = H - padT - padB

  const wMin = Math.min(...pts.map((p) => p.w))
  const wMax = Math.max(...pts.map((p) => p.w))
  let yMin = wMin
  let yMax = wMax
  if (yMin === yMax) { yMin -= 0.5; yMax += 0.5 }

  const t0 = pts[0].t
  const t1 = pts[pts.length - 1].t

  const points = pts.map((p) => {
    const x = t1 === t0 ? padL + innerW / 2 : padL + ((p.t - t0) / (t1 - t0)) * innerW
    const y = padT + ((yMax - p.w) / (yMax - yMin)) * innerH
    return { x: round1(x), y: round1(y), date: p.date, w: p.w }
  })

  const grid = [0, 0.5, 1].map((frac) => {
    const val = yMax - frac * (yMax - yMin)
    return { y: round1(padT + frac * innerH), label: val.toFixed(1) }
  })

  return {
    W,
    H,
    padL,
    right: W - padR,
    bottom: H - padB,
    points,
    line: points.map((p) => `${p.x},${p.y}`).join(' '),
    grid,
    firstLabel: pts[0].date.slice(5),
    lastLabel: pts[pts.length - 1].date.slice(5),
    count: pts.length
  }
})

async function fetchMetrics() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.listBodyMetrics()
    metrics.value = (data || []).slice().sort((a, b) => (a.measureDate < b.measureDate ? -1 : 1))
  } catch (e) {
    error.value = '加载身体指标失败，请确认后端可访问后重试。'
  } finally {
    loading.value = false
  }
}

function openForm() {
  form.value = emptyForm()
  formOpen.value = true
}

function closeForm() {
  formOpen.value = false
}

async function submit() {
  const f = form.value
  if (!f.measureDate || f.weightKg === '') {
    error.value = '请填写测量日期和体重。'
    return
  }
  const payload = {
    measureDate: f.measureDate,
    weightKg: Number(f.weightKg),
    chestCm: toNum(f.chestCm),
    waistCm: toNum(f.waistCm),
    hipCm: toNum(f.hipCm),
    armCm: toNum(f.armCm)
  }

  saving.value = true
  error.value = ''
  try {
    await api.recordBodyMetric(payload)
    closeForm()
    await fetchMetrics()
  } catch (e) {
    error.value = '记录失败，请检查输入后重试。'
  } finally {
    saving.value = false
  }
}

onMounted(fetchMetrics)
</script>

<style scoped>
.body {
  max-width: 860px;
}
.stat-empty {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.chart-panel {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px 22px;
  margin-top: 20px;
}
.chart-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.chart-title {
  font-size: 15px;
  font-weight: 700;
}
.chart-sub {
  color: var(--muted);
  font-size: 13px;
}
.chart {
  display: block;
  width: 100%;
  height: auto;
}
.chart .grid-line {
  stroke: var(--line);
  stroke-width: 1;
}
.chart .line {
  stroke: var(--accent);
  stroke-width: 2;
}
.chart .dot {
  fill: var(--accent);
}
.chart .tick {
  fill: var(--muted);
  font-size: 11px;
}
</style>