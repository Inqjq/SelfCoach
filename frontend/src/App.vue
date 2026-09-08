<template>
  <main class="app">
    <h1>SelfCoach 自律教练</h1>
    <p>前后端分离骨架已就绪。</p>
    <p class="hint">后端健康检查：<code>/api/health</code>（已代理到 :8080）</p>
  </main>
</template>

<script setup>
// 骨架阶段暂无业务逻辑。
// 后续对话界面（Phase 0 的 F-05）会在这里实现。
</script>

<style scoped>
.app {
  max-width: 720px;
  margin: 0 auto;
  padding: 2rem;
  font-family: system-ui, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #1f2328;
}
h1 {
  font-size: 1.5rem;
}
.hint {
  color: #59636e;
  font-size: 0.9rem;
}
code {
  background: #f0f1f2;
  padding: 0.1em 0.4em;
  border-radius: 4px;
}
</style>