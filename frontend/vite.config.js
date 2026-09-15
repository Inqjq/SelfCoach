import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前后端分离：前端跑在 http://localhost:5173，后端在云服务器 8081。
// /api 统一代理到后端，前端代码里直接写相对路径（如 POST /api/chat）。
// 后端地址通过环境变量 BACKEND_HOST 配置（写在 frontend/.env.local，已 gitignore，不上库）。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.BACKEND_HOST || 'http://localhost:8081'

  if (mode === 'development' && !env.BACKEND_HOST) {
    console.warn(
      '[SelfCoach] 未配置 BACKEND_HOST，/api 将代理到 http://localhost:8081（本地后端）。' +
      '联调云服务器请在 frontend/.env.local 填写 BACKEND_HOST，然后重启 npm run dev。'
    )
  }

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true
        }
      }
    }
  }
})