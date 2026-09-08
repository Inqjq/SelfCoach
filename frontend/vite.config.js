import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前后端分离：后端在 http://localhost:8080，前端在 http://localhost:5173
// /api 请求代理到后端，联调时前端代码里直接写相对路径即可。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})