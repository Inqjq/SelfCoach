<template>
  <section class="chat">
    <header class="view-head">
      <h1>知识问答</h1>
      <p>基于训练知识库回答，每条回答标注来源；超出知识范围会明确说明。</p>
    </header>

    <div class="thread" ref="listEl">
      <div v-if="!messages.length && !loading" class="empty">
        <p class="kicker">从这里开始</p>
        <h2>问一个训练问题</h2>
        <p>例如「深蹲主要锻炼哪些肌群？」或「减脂期每天该吃多少蛋白质？」</p>
      </div>

      <div v-for="m in messages" :key="m.id" class="msg" :class="m.role">
        <div class="bubble" :class="m.role">
          <!-- 用户消息：纯文本 -->
          <p v-if="m.role === 'user'" class="answer">{{ m.text }}</p>

          <!-- AI 回答：文本（[n] 渲染为引用角标）+ 来源列表 -->
          <template v-else>
            <p class="answer">
              <template v-for="(part, i) in splitCitations(m.text)" :key="i">
                <sup v-if="part.type === 'cite'" class="cite">{{ part.value }}</sup>
                <span v-else>{{ part.value }}</span>
              </template>
            </p>

            <div v-if="m.sources.length" class="sources">
              <div class="sources-head">依据 {{ m.sources.length }} 条来源</div>
              <div v-for="s in m.sources" :key="s.idx" class="source">
                <button type="button" class="source-head" @click="s.open = !s.open">
                  <span class="s-badge" :class="{ on: s.open }">{{ s.idx }}</span>
                  <span class="s-doc">{{ s.docId }} · 块 {{ s.chunkIndex }}</span>
                  <span class="s-caret">{{ s.open ? '−' : '+' }}</span>
                </button>
                <div v-if="s.open" class="s-content">{{ s.content }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <div v-if="loading" class="msg assistant">
        <div class="bubble assistant loading">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div v-if="error" class="error" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="error-close" @click="error = ''">收起</button>
    </div>

    <form class="composer" @submit.prevent="send">
      <input
        v-model="input"
        class="input"
        type="text"
        placeholder="问一个健身问题…"
        :disabled="loading"
        autocomplete="off"
      />
      <button type="submit" class="btn" :disabled="loading || !input.trim()">发送</button>
    </form>
  </section>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { api } from '../api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const error = ref('')
const listEl = ref(null)

let seq = 0

// 把回答文本按 [n] 引用标记拆成「普通文本 / 引用角标」片段。
// 用纯拆分渲染而非 v-html，避免后端/模型内容被当作 HTML 注入执行。
function splitCitations(text) {
  const parts = []
  const re = /(\[\d+\])/g
  let last = 0
  let m
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) parts.push({ type: 'text', value: text.slice(last, m.index) })
    parts.push({ type: 'cite', value: m[1] })
    last = m.index + m[1].length
  }
  if (last < text.length) parts.push({ type: 'text', value: text.slice(last) })
  return parts
}

async function scrollToBottom() {
  await nextTick()
  if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
}

async function send() {
  const question = input.value.trim()
  if (!question || loading.value) return

  messages.value.push({ id: ++seq, role: 'user', text: question, sources: [] })
  input.value = ''
  loading.value = true
  error.value = ''

  try {
    const data = await api.chat(question)
    messages.value.push({
      id: ++seq,
      role: 'assistant',
      text: data.answer ?? '',
      sources: (data.sources || []).map((s, i) => ({
        docId: s.docId,
        chunkIndex: s.chunkIndex,
        content: s.content,
        idx: i + 1,
        open: false
      }))
    })
  } catch (e) {
    error.value = '暂时连不上问答服务，请确认后端可访问后重试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 840px;
  margin: 0 auto;
  padding: 0 40px;
}
.chat .view-head {
  flex: none;
  padding-top: 28px;
}

.thread {
  flex: 1;
  overflow-y: auto;
  padding: 22px 0 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.thread .empty {
  margin: auto 0;
}

.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg.assistant { justify-content: flex-start; }

.bubble {
  max-width: 80%;
  padding: 11px 14px;
  border-radius: 4px;
  font-size: 14.5px;
  line-height: 1.65;
  word-break: break-word;
}
.bubble.user {
  background: var(--accent);
  color: var(--accent-ink);
}
.bubble.assistant {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--line);
}
.answer {
  margin: 0;
  white-space: pre-wrap;
}
.cite {
  color: var(--accent);
  font-weight: 700;
  font-size: 0.72rem;
  margin: 0 1px;
}

/* 来源 */
.sources {
  margin-top: 10px;
  border-top: 1px solid var(--line);
  padding-top: 8px;
}
.sources-head {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
.source + .source { margin-top: 2px; }
.source-head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  background: transparent;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  color: var(--text);
  font-size: 13px;
  text-align: left;
}
.source-head:hover { background: var(--panel-2); }
.s-badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line);
  border-radius: 3px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
}
.s-badge.on {
  background: var(--accent);
  color: var(--accent-ink);
  border-color: var(--accent);
}
.s-doc {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--muted);
}
.s-caret { color: var(--muted); font-size: 13px; }
.s-content {
  margin: 4px 0 4px 26px;
  padding: 8px 10px;
  background: var(--panel-2);
  border-radius: 3px;
  font-size: 13px;
  color: var(--text);
  white-space: pre-wrap;
}

/* 异常 */
.error {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 1px solid var(--accent);
  border-left: 3px solid var(--accent);
  border-radius: var(--radius);
  font-size: 14px;
}
.error-close {
  background: none;
  border: none;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
}
.error-close:hover { color: var(--accent); }

/* 输入 */
.composer {
  flex: none;
  display: flex;
  gap: 10px;
  padding-bottom: 26px;
}
.composer .input { flex: 1; border-radius: 4px; }

/* loading */
.loading {
  display: inline-flex;
  gap: 5px;
  padding: 15px 18px;
}
.loading span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: blink 1.2s infinite ease-in-out;
}
.loading span:nth-child(2) { animation-delay: 0.2s; }
.loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
}
</style>