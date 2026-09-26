# Sprint-6: DTS Wiki v1 —— 以 PG 为事实源、与产品 git 双向同步的知识协作平台

**设计文档**: [`features/F0-基线与技术选型spike/design/`](features/F0-基线与技术选型spike/design/)（00–08；编码由独立会话按 `08-编码任务与交接说明.md` 执行，代码在子模块 `dts-wiki`）
**时间**: 2026-10 ~ 2026-11（时间盒假设：2026-10-12 至 2026-11-20，约 5–6 周；与 Sprint-5 并行，互不阻塞）
**状态**: DRAFT
**类型**: Implementation（新产品：内部协作工具 → 后续演进为 DTS 知识中心）
**目标**: 团队成员用统一账号登录 wiki，在自己有权限的产品空间里浏览、搜索、编辑、追溯页面；
所有内容以 **PostgreSQL 为唯一事实源**；各产品 git 仓库中的 `docs/`、`worklog/` 与 wiki **双向同步**——
网页编辑在 1 分钟内成为该产品仓库中作者为编辑者的提交，开发者/AI 推送到仓库的修改在 1 分钟内成为 wiki 中的新版本，
两边同时修改同一页面时不静默覆盖，进入人工合并。

## 背景与价值
- 现有 wiki（`dts-rdc/wiki/` + `deploy/wiki/`，2026-09-26 上线）是"git 内容的静态展示 + 编辑入口"，已具备：Keycloak 统一登录、
  产品级隔离、网页编辑、图片上传、git 同步。但它的上限明显：无页面管理（改名/移动/删除）、无历史对比界面、无评论通知、
  每次保存全站重建约 30 秒、只能表达 git 里的 Markdown 文件（会议纪要等非研发内容无处安放）。
- 目标是向 Confluence 式协作平台演进，并成为 DTS 知识中心的雏形（带权限的知识库 = AI 头脑 RAG 的合规数据源，铁律 #3）。
  因此存储改为 PG；研发文档仍需与产品仓库保持一致（AI 助手、sprint-workflow、开发者直接读写 git），故 git 作为"同步端"保留。
- 本期**安全与权限需求降级**：只做到产品（空间）一级；页面级权限、审计报表、外部分享等不做。

## 架构决策记录 (ADR)

| # | 决策点 | 选择 | 状态 | 理由 | 影响 |
|---|--------|------|------|------|------|
| W-ADR-1 | 事实源 | **PostgreSQL** 存全部页面、版本、附件元数据、评论；git 仅为研发文档的同步端 | 已定（用户 2026-09-26） | Confluence 式功能（评论、页面管理、历史界面、非研发页面）需要数据库 | 需要同步器与冲突模型（F5） |
| W-ADR-2 | 技术栈 | **JHipster 9 单体**（用户 2026-09-26 指定）：生成器 9.2.x（按 R-012 取首发满 30 天的最新次版本线）、Spring Boot 由 JHipster 管理（9.2 = 3.5.15，不单独升级 Boot 4）、**Java 25**；后端 JHipster 生成（`skipClient`），前端为 `frontend/` 独立的 **React 19 + antd 6**（用户 2026-09-26 指定），打进同一个 jar | 已定 | 与 dts-stack（JHipster 后端 + React/antd 前端）同架构；实体/Liquibase/OAuth2/测试脚手架现成 | 设计见 `features/F0-基线与技术选型spike/design/`；实体以 `dts-wiki/jhipster/dts-wiki.jdl` 为准 |
| W-ADR-3 | 内容规范格式 | **Markdown 原文**（PG 存 Markdown，与 git 字节级一致） | 已定 | 双向同步无损，避免富文本↔Markdown 转换产生伪冲突 | 编辑器须 Markdown 原生（F0/T02 选型） |
| W-ADR-4 | 代码位置 | 新仓库 **`billyhotjava/dts-wiki`**，作为 dts-rdc submodule | 已定 | 边界清晰，日后整体并入 dts-studio 知识中心 | 需在 GitHub 建仓（用户操作） |
| W-ADR-5 | 权限粒度 | **仅产品（空间）级**：沿用 Keycloak realm `yuzhicloud`、client `dts-wiki` 的角色 `space-<slug>`（读）、`editor`（写）、`admin`；**不做页面级权限** | 已定（降级） | 与现网 wiki 权限模型一致，迁移零成本 | F2 |
| W-ADR-6 | 身份接入 | JHipster `authenticationType: oauth2`（服务端会话 + OIDC 授权码），不再依赖 oauth2-proxy | 已定 | 应用需要细粒度判断角色、生成审计作者；少一跳代理 | 现网 oauth2-proxy 随切换下线 |
| W-ADR-7 | 同步语义 | 页面分两类：**git 绑定页**（空间配置的仓库路径下的 `.md`，双向同步）与 **wiki 原生页**（仅 PG）；同步单位 = 文件；冲突 = 自上次同步以来两边都改 → 标记冲突、保留两版、人工三方合并；**不自动覆盖** | 提议（F5/T01） | 沿用现网"本地提交 + 定时 rebase/push"的成功经验，把冲突显式化 | F5 |
| W-ADR-8 | 中文全文检索 | PG 内实现：候选 **pg_bigm**（二元组，零词典）或 **zhparser**（分词）；由 spike 决定 | 待定（F0/T03） | 不引入额外搜索集群 | 可能需要自定义 PG 镜像 |
| W-ADR-9 | 附件存储 | v1 放宿主机卷（`/data/dts-wiki/attachments`，按 sha256 去重）；接口按 S3 抽象，后续切 SeaweedFS | 提议（F4/T03） | 先简后繁；R-012 已定对象存储走 S3 API | — |

## 端到端契约链 (Vertical Slice)
主竖线："梅卫锋（产品-PRS 成员 + 研发部）在 wiki 编辑 PRS 的一个 worklog 页面，1 分钟内 prs-stack 仓库出现他署名的提交；
开发者随后在仓库修改同一目录另一文件并 push，1 分钟内 wiki 出现新版本。"

| 层 | 契约/落点 | 签名要点 |
|----|-----------|----------|
| UI | `/s/prs/pages/{pageId}`（页面阅读）→"编辑"→ `/s/prs/pages/{pageId}/edit` | 页面树、面包屑、版本号、"来自 git：prs-stack@abc123"标识 |
| 登录 | Keycloak `yuzhicloud`，client `dts-wiki`（授权码 + PKCE，回调 `/login/oauth2/code/keycloak`） | 角色 `dts-wiki:space-prs`、`dts-wiki:editor` |
| API | `GET /api/spaces/{slug}/pages/{id}`、`PUT /api/pages/{id}`（`{baseVersion, content, message}` → 200 `{version}` / 409 `{currentVersion}`） | 乐观并发：`baseVersion` = 编辑开始时的版本号 |
| Service | `PageService.update` → 写 `page_version`（source=`WEB`）→ 发布 `PageChanged` 事件 → `GitSyncService` 入队 | 事务内写库；同步异步进行 |
| 数据 | `page`（`space_id, parent_id, title, git_path, current_version_id`）、`page_version`（`content_md, author, source, git_commit`）、`sync_binding`、`sync_state` | uk(space_id, git_path) |
| 同步 | `GitSyncService`：PG→git 提交（author=用户）→ rebase → push；git→PG：fetch → diff 自 `last_synced_commit` → 新版本（source=`GIT`） | 冲突 → `page.sync_status=CONFLICT` |
| 迁移 | Liquibase `db/changelog/*.xml` | 首次同步导入 dts-rdc（docs/、worklog/）与 prs-stack（worklog/） |

## 现状勘察账本 (Context Ledger)

| # | 事实 | 证据 |
|---|------|------|
| 1 | 现网 wiki：`dts-rdc/wiki/`（VitePress 站点 + `server/` Node API 约 800 行）、`deploy/wiki/`（oauth2-proxy + nginx + api），`https://wiki.yuzhicloud.com`，.50:`/data/dts-wiki` | 提交 `c0b7807`、`9885b05` |
| 2 | 现网可复用思路/代码：乐观并发（`pages.mjs` writePage/baseSha）、图片按页面目录存放、每次编辑一个 git 提交（作者=SSO 用户）、定时 fetch→rebase→push 且冲突时中止（`git.mjs` syncWithRemote）、产品空间映射（`spaces.mjs`）、单元测试 | `wiki/server/*.mjs` |
| 3 | 产品注册表 `dts-rdc/products.json`：dts（docs→`docs`，worklog→`worklog`）、prs（`products/prs/docs`、`products/prs/worklog`）、extras（sandbox）。**新方案中 PRS 改为同步 `prs-stack` 仓库** | `products.json` |
| 4 | Keycloak：realm `yuzhicloud`；client `dts-wiki`（机密客户端，redirect 仅 `https://wiki.yuzhicloud.com/oauth2/callback`）；client 角色 reader/editor/admin/space-dts/space-prs；组 管理员/研发部/产品-DTS 平台/产品-PRS 花卉租赁 | `deploy/sso/bootstrap/realm-yuzhicloud.sh`、`apps/wiki-spaces.sh` |
| 5 | 内容规模：dts-rdc `docs/`+`worklog/` 约 201 个 md，4.5 MB；含中文目录/文件名、少量 pdf/pptx；prs-stack 有 `worklog/`（sprint-1-202609、integration-20260926），无 `docs/` | `find`/`du` |
| 6 | prs-stack：`git@github.com:billyhotjava/prs-stack.git`，已检出于 `dts-rdc/dts-app-stack/prs-stack`（backend/frontend/pack/sources/worklog） | `git remote` |
| 7 | .50 环境：RHEL 8.10、Docker 26、**docker daemon 代理坏（192.168.1.54），镜像须本机 `docker save` 传输**，不可重启 dockerd（live-restore 关，jira 会跟着重启）；可达 npmmirror、GitHub(ssh)，GitHub https 不通 | `reference_infra_topology` 记忆 |
| 8 | .50 现有 PG：`devops-postgres`(15.3, jira 用)、`dts-sso-db`(17, keycloak)。wiki 应**独立实例**（PG 18）并纳入每日备份（参照 `deploy/sso/backup/`） | `docker ps` |
| 9 | GitHub deploy key：现网 `/data/dts-wiki/secrets/deploy_key` 已生成但**尚未加到 GitHub**；新方案每个同步仓库（dts-rdc、prs-stack）都需要一把有写权限的 key | 状态页 sync=offline |
| 10 | sprint-workflow 规范：`v{x}/sprint-{N}-{YYYYMM}/features/F{N}-*/T{NN}-*.md`；README 为目录索引页 | `.claude/skills/sprint-workflow` |

**开放问题**：
- Q1 PRS 的研发文档是否也需要 `docs/`（prs-stack 目前只有 worklog/）？→ F5/T02 配置时确认
- Q2 wiki 原生页（非 git）是否也要定期导出备份到 git（作为只读快照）？→ F8/T02
- Q3 dts-rdc 中 `products/prs/`（现网临时 PRS 空间）内容迁往 prs-stack 还是删除？→ F8/T03

## Gate Registry

| Gate | 项目 | 状态 | 证据 | 未过则关联 Task |
|------|------|------|------|-----------------|
| G0 | 交付基线（空仓库可构建、可部署到 .50、可登录） | PENDING | `it/baseline.md` | F0/T01、F1/T04 |
| G0 | 领域不变量（与 DTS worklog/docs 规范一致；铁律 #1 可降级：wiki 挂了研发文档仍在 git） | PASS | 本文档 W-ADR-1/7 | — |
| G1 | 契约链贯通 | GAP | 本文档 §端到端契约链；API 细节在 F1/F3/F5 | F1/T02、F5/T01 |
| G1 | 非功能预算 | PENDING | `assets/nfr-budget.md`（页面打开 P95 < 500ms、保存 < 1s、同步延迟 < 60s、检索 P95 < 800ms） | F9/T01 |
| G3 | 发布安全（与现网 wiki 并行、可回退） | PENDING | `assets/release-plan.md` | F9/T03 |
| G4 | 可运维性（备份、告警、runbook） | PENDING | `assets/runbook.md` | F9/T02 |
| G4 | DoD 验收 | PENDING | `it/` | F9/T04 |

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 |
|----|---------|---------|--------|------|
| F0 | 基线与技术选型 spike | 4 | P0 | DRAFT |
| F1 | 仓库、数据模型与应用骨架 | 4 | P0 | DRAFT |
| F2 | 统一登录与产品级权限 | 3 | P0 | DRAFT |
| F3 | 空间与页面管理 | 5 | P0 | DRAFT |
| F4 | 编辑器、附件与模板 | 4 | P0 | DRAFT |
| F5 | Git 双向同步 | 6 | P0 | DRAFT |
| F6 | 版本历史与追溯 | 3 | P0 | DRAFT |
| F7 | 搜索与导航 | 3 | P1 | DRAFT |
| F8 | 协作：评论、@提及、通知 | 3 | P1 | DRAFT |
| F9 | 部署、迁移切换与运维 | 4 | P0 | DRAFT |

**依赖顺序**: F0 → F1 → F2 → {F3, F4} → F5 → F6 → F7 ∥ F8 → F9
**关键路径**: F0/T01 → F1/T02 → F3/T01 → F5/T03 → F5/T04 → F9/T03

## 追溯矩阵

| 需求点 | Feature | 关键 Task | 验收证据 |
|--------|---------|-----------|----------|
| PG 为唯一事实源 | F1、F5 | F1/T02、F5/T05 | `it/IT-01-schema.md` |
| 研发文档与产品 git 双向一致 | F5 | F5/T03、F5/T04、F5/T06 | `it/IT-03-sync.md` |
| Confluence 式页面管理/历史/评论 | F3、F6、F8 | F3/T02、F6/T02、F8/T01 | `it/IT-04-pages.md` |
| 产品级权限（降级后） | F2 | F2/T02 | `it/IT-02-access.md` |
| 平滑替换现网 wiki | F9 | F9/T03 | `it/IT-05-cutover.md` |

## 完成标准
- [ ] 主竖线在 .50 运行实例上跑通（两个方向各一次），证据入 `it/IT-03-sync.md`
- [ ] 现网 wiki 的全部内容（dts-rdc docs/worklog、prs-stack worklog、练习区）已导入，页面数与文件数一致
- [ ] 同时修改同一页面产生冲突时：两个版本都保留、界面可三方合并、合并结果同步回 git
- [ ] 页面改名/移动/删除在 git 中表现为 `git mv`/`git rm` 提交
- [ ] 旧地址（`/p/<slug>/...`、`/docs/...`）重定向到新地址；现网 wiki 下线且可回退

## 非目标
- 页面级权限、外部/匿名分享、审计报表、访问统计
- 多人实时协同编辑（W-ADR-3 已为其预留：Markdown 原生编辑器可在后续接 Yjs）
- PDF/Word 导出、Confluence 数据导入、移动端专门适配
- 作为 DTS 知识中心的 RAG 接入（后续 Sprint，在 dts-studio 规划中承接）
