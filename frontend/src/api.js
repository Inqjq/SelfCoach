// 统一的后端调用入口：所有请求走 /api 相对路径，由 Vite dev 代理转发到 BACKEND_HOST。
// 后端只讲 JSON；DELETE 成功返回 204（无响应体），这里单独处理。

async function request(path, { method = 'GET', body } = {}) {
  const options = { method, headers: {} }
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  const res = await fetch(path, options)

  if (res.status === 204) return null
  if (!res.ok) {
    throw new Error(`请求失败（HTTP ${res.status}）`)
  }
  return res.json()
}

function query(params = {}) {
  const qs = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== '')
  ).toString()
  return qs ? `?${qs}` : ''
}

export const api = {
  // 知识问答
  chat(question) {
    return request('/api/chat', { method: 'POST', body: { question } })
  },

  // 训练记录（F-01）
  listTrainingLogs(params) {
    return request(`/api/training-logs${query(params)}`)
  },
  createTrainingLog(log) {
    return request('/api/training-logs', { method: 'POST', body: log })
  },
  updateTrainingLog(id, patch) {
    return request(`/api/training-logs/${id}`, { method: 'PUT', body: patch })
  },
  deleteTrainingLog(id) {
    return request(`/api/training-logs/${id}`, { method: 'DELETE' })
  },

  // 身体指标（F-02）
  listBodyMetrics(params) {
    return request(`/api/body-metrics${query(params)}`)
  },
  recordBodyMetric(metric) {
    return request('/api/body-metrics', { method: 'POST', body: metric })
  },
  latestBodyMetric() {
    return request('/api/body-metrics/latest')
  }
}