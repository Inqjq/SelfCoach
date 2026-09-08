# SelfCoach — 项目导航 & 协作规范（给 AI 协作者）

> 这个文件是给「接手的 AI 助手（或新窗口会话）」看的项目导航与协作规范。
> 新窗口开工前，先读完本文件，再读 `docs/` 下三份文档（需求、技术、开发计划）。

## 1. 项目是什么
- **SelfCoach（自律教练）**：一个个人 AI 自律健身教练 Agent，Java 实现。
- 定位：求职简历项目 + 作者本人长期自用，覆盖 RAG / Agent / 记忆 / 安全护栏 / 可观测评估五大能力。

## 2. 当前状态
- ✅ 文档已完成：需求 / 技术 / 开发计划，另新增《进度文档》记录推进状态。
- ✅ 前后端分离骨架已搭建（backend: Spring Boot 3 + WebFlux；frontend: Vue 3 + Vite；docker-compose: PostgreSQL 16 + pgvector）。
- 🚧 业务功能尚未开始，**下一步是 Phase 0**：建表 + 最小 RAG 闭环。

## 3. 已定关键决策（不要推翻，除非用户明确要求）
- 语言：**Java**（不是 Python）。
- 技术栈：Spring Boot 3 + WebFlux + Spring AI/LangChain4j + PostgreSQL/pgvector + Lucene(BM25) + Langfuse。
- 架构：**模块化单体**（不是微服务）；进阶再加一个 Python 侧车跑 embedding/rerank 模型。
- 交互形态：**前后端分离**（前端 Vue 3 + Vite，后端只出 REST + SSE API），已取代早期的 CLI / Thymeleaf 方案。
- 向量库：**pgvector**（不用 Milvus）。
- **RAG 必须做**（知识问答带引用是核心）。

## 4. 作者协作偏好（协作时请注意）
- 作者主栈 Java，喜欢手写代码，**要能讲清每一行代码**。
- 中文沟通；核心逻辑尽量手写自研，基础设施用现成库。

## 5. 协作规范（给 AI）
- **一次只写一个功能**，写完解释清楚、等用户确认，再进下一个；不要一次性堆大量代码或并行铺开。
- 核心逻辑尽量手写自研，基础设施用现成库。
- 用中文解释技术细节。
- **代码提交 / 推送前必须先问用户**，不要自主 push。

## 6. 下一步（Phase 0 起点）
- 搭 Spring Boot 3 + WebFlux 骨架；建表（user_profile / training_log / body_metric / doc_chunk）；跑通「喂文档 → 提问 → 带引用回答」的最小 RAG 闭环。
- 详细任务、验收标准、简历产出见 `docs/开发计划.md`。

## 7. SelfCoach 教练 Agent 核心设计（产品侧要点）
- **控制域（重要）**：只做训练 / 营养 / 恢复，**不碰医疗诊断**；命中禁区时明确拒答并提示就医。
- 能力清单（按需求文档 F 编号）：训练记录、身体指标、知识问答(RAG 带引用)、计划生成、打卡复盘、长期记忆、安全护栏、评估。
- 优先级 P0 → P4 见 `docs/需求文档.md`。