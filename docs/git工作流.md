# SelfCoach Git 分支工作流（Feature Branch · 企业标准）

> 采用业界标准的 **Feature Branch Workflow**：`main` 为稳定主干，每个功能在独立分支开发，完成验证后合并回 `main`。本项目用多个 Claude 窗口模拟团队协作，每个窗口相当于一个成员、各自认领一条功能分支。

## 1. 分支模型

| 分支 | 说明 | 是否可直接推送 |
|---|---|---|
| `main` | 主分支，始终稳定、可部署 | ❌ 禁止，改动走 PR 合并 |
| `feature/<功能名>` | 功能分支，从 `main` 切出 | ✅ 可推送共享 |

（`release/*`、`hotfix/*` 属更重的 Git-Flow 模型，本项目按 Phase 推进阶段使用，暂不启用主分支之外的长期分支。）

## 2. 当前分支规划（Phase 0）

| 分支 | 功能 | 对应里程碑 |
|---|---|---|
| `feature/training-log` | F-01 训练日志 CRUD | 第 3 步 |
| `feature/body-metric` | F-02 身体指标记录 | 第 4 步 |
| `feature/rag` | F-03 知识库问答(RAG) | 第 5–7 步 |
| `feature/chat-ui` | F-05 对话界面 | 第 8 步 |

## 3. 命名与提交规范

- **分支命名**：`feature/` + 小写 + 连字符，如 `feature/training-log`，一眼看出功能。
- **提交消息**：采用 Conventional Commits：
  - `feat:` 新功能　`fix:` 修复　`docs:` 文档　`chore:` 维护　`refactor:` 重构　`test:` 测试

## 4. 每个功能的标准流程

1. 从最新的 main 切出分支：
   `git checkout main && git pull && git checkout -b feature/xxx`
2. 在分支上开发，多次小步提交。
3. 推送到远端共享（其他窗口可见）：
   `git push -u origin feature/xxx`
4. 完成并验证后，合并回 main（用 `--no-ff` 保留合并记录，历史清晰）：
   `git checkout main && git pull`
   `git merge --no-ff feature/xxx`
   `git push origin main`
5. 清理已合并分支：`git branch -d feature/xxx`

## 5. 多窗口（= 团队）协作约定

本项目用多个 Claude 窗口分担功能，协同原则：

- **每个窗口只在自己的分支上工作**，互不覆盖，天然隔离 = 「避免混在一起」。
- 共享靠远端：各自随时 `git push` 自己的分支、`git pull` 同步 main。
- **合并前的 review**：在另一个窗口 / GitHub PR 里检查改动，再合入 main。
- main 上任何时刻都应是「可跑的稳定状态」。

## 6. main 分支保护（GitHub 侧，一件手工配置）

去 GitHub 仓库 **Settings → Branches → Add branch protection rule**，选 `main`，勾选：
- Require pull request reviews before merging（合并前要求 review）
- 不勾选「允许直接 push」/ 可关掉 force push

这样进一步保证 main 干净。本地先建分支与推送，保护规则建议由主账号在网页上开启。