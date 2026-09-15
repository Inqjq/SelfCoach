<template>
  <div class="shell">
    <aside class="side">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">SC</span>
        <div class="brand-text">
          <div class="brand-name">SelfCoach</div>
          <div class="brand-sub">自律教练 · 训练面板</div>
        </div>
      </div>

      <nav class="nav">
        <RouterLink class="nav-item" to="/chat">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span>知识问答</span>
        </RouterLink>

        <RouterLink class="nav-item" to="/training">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M14.4 14.4 9.6 9.6" />
            <path d="M18.657 21.485a2 2 0 1 1-2.829-2.828l-1.767 1.768a2 2 0 1 1-2.829-2.829l6.364-6.364a2 2 0 1 1 2.829 2.829l-1.768 1.767a2 2 0 1 1 2.828 2.829z" />
            <path d="m21.5 21.5-1.4-1.4" />
            <path d="M3.9 3.9 2.5 2.5" />
            <path d="M6.404 12.768a2 2 0 1 1-2.829-2.829l1.768-1.767a2 2 0 1 1-2.828-2.829l2.828-2.828a2 2 0 1 1 2.829 2.828l1.767-1.768a2 2 0 1 1 2.829 2.829z" />
          </svg>
          <span>训练记录</span>
        </RouterLink>

        <RouterLink class="nav-item" to="/body">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
            <polyline points="16 7 22 7 22 13" />
          </svg>
          <span>身体指标</span>
        </RouterLink>
      </nav>

      <div class="side-foot">记录每一次训练，让自律有迹可循</div>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
}

.side {
  flex: none;
  width: 236px;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  border-right: 1px solid var(--line);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 20px;
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: var(--accent);
  color: var(--accent-ink);
  font-weight: 800;
  font-size: 14px;
  letter-spacing: 0.5px;
  border-radius: 4px;
}
.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.brand-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 1px;
}

.nav {
  display: flex;
  flex-direction: column;
  margin-top: 6px;
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  border-radius: var(--radius);
}
.nav-item:hover {
  color: var(--text);
  background: var(--panel);
}
.nav-item.router-link-active {
  color: var(--text);
  background: var(--panel);
}
.nav-item.router-link-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  background: var(--accent);
}
.nav-item svg {
  flex: none;
  color: var(--muted);
}
.nav-item.router-link-active svg {
  color: var(--accent);
}

.side-foot {
  margin-top: auto;
  padding: 18px 20px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 12px;
}

.content {
  flex: 1;
  overflow-y: auto;
}

@media (max-width: 760px) {
  .shell {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }
  .side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    border-right: none;
    border-bottom: 1px solid var(--line);
  }
  .brand { padding: 12px 16px; }
  .brand-sub { display: none; }
  .nav {
    flex-direction: row;
    margin: 0 0 0 auto;
  }
  .nav-item { padding: 13px 14px; }
  .nav-item.router-link-active::before {
    left: 8px;
    right: 8px;
    top: auto;
    bottom: 0;
    width: auto;
    height: 3px;
  }
  .side-foot { display: none; }
  .content { overflow: visible; }
}
</style>