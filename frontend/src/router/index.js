import { createRouter, createWebHashHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import TrainingLogView from '../views/TrainingLogView.vue'
import BodyMetricView from '../views/BodyMetricView.vue'
import PlanView from '../views/PlanView.vue'

// 用 hash 模式：路由信息放在 # 后面，不依赖服务器回退配置，
// dev / 直接开 dist/index.html / 无 fallback 的静态托管都能正常切换。
const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', name: 'chat', component: ChatView },
  { path: '/training', name: 'training', component: TrainingLogView },
  { path: '/body', name: 'body', component: BodyMetricView },
  { path: '/plan', name: 'plan', component: PlanView },
  { path: '/:pathMatch(.*)*', redirect: '/chat' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router