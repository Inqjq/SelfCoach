# SelfCoach — 个人 AI 自律健身教练

一个面向个人使用的 AI「自律 + 健身」教练 Agent，也是我的求职简历项目。用 Java 从零实现 **RAG + Agent + 长期记忆 + 安全护栏 + 可观测/评估** 的完整闭环，定位是「Java 后端工程师自研大模型 / Agent 能力」的实战作品。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 17 + Spring Boot 3 + WebFlux |
| LLM / Agent | Spring AI（可替换 LangChain4j） |
| 数据 | PostgreSQL 16 + pgvector |
| 检索 | Lucene（BM25）+ 向量检索 + 重排 |
| 分词 | HanLP / IK Analyzer |
| Embedding | API（Qwen Embedding 等），进阶加 Python 侧车跑 bge-m3 |
| 可观测 | Langfuse（Java SDK） |
| 部署 | Docker + docker-compose |

## 文档

- [需求文档](docs/需求文档.md)
- [技术文档](docs/技术文档.md)
- [开发计划](docs/开发计划.md)
- [进度文档](docs/进度.md)

## 进度

- ✅ 前后端分离骨架已搭建（backend: Spring Boot 3 + WebFlux；frontend: Vue 3 + Vite）。
- 🚧 Phase 0 进行中，下一步：建表 + 最小 RAG 闭环。详见 [进度文档](docs/进度.md)。