# DTS Wiki v1 设计文档 · 00 概述与架构决策

> 读者：负责编码的会话 / 开发者。本目录（dts-rdc 仓库 `worklog/v1.0.0/sprint-6-202610/features/F0-基线与技术选型spike/design/`）是实现的唯一依据；与之冲突的口头描述以本文档为准。
> 规划与任务拆分见同一 Sprint 的 `README.md` 与各 Feature/Task（编号 F0–F9 与本文档一一对应）。代码在 `dts-wiki` 仓库（dts-rdc 的子模块）。

## 1. 目标

面向团队内部的 Confluence 式知识协作平台，并作为日后 DTS 知识中心的雏形：

1. 统一账号（Keycloak）登录，按**产品空间**隔离可见内容；
2. 页面树浏览、Markdown 所见即所得编辑、图片/附件、模板、版本历史（对比、恢复）、评论、@提及、通知、全文检索；
3. **PostgreSQL 为唯一事实源**；各产品 git 仓库中的研发文档（`docs/`、`worklog/`）与 wiki **双向同步**（≤ 60 s），冲突不静默覆盖；
4. 替换现网静态 wiki（`dts-rdc/wiki/`，2026-09-26 上线），域名 `wiki.yuzhicloud.com` 不变，旧链接可跳转。

**非目标（v1）**：页面级权限、匿名/外部分享、实时多人协同编辑、PDF/Word 导出、Confluence 导入、AI/RAG 接入。

## 2. 架构决策（ADR）

| # | 决策 | 选择 | 理由 |
|---|------|------|------|
| D1 | 工程架构 | **DTS Wiki 9 单体（monolith），`skipClient`**：后端由 DTS Wiki 生成；前端为同仓库 `frontend/` 下独立的 React + antd 应用，构建时打进同一个 jar，一个 jar 同时提供页面与 API | 与 DTS 平台一致（dts-stack 各服务均为 DTS Wiki 生成且 skipClient，前端另用 React + antd）；实体、Liquibase、OAuth2、测试脚手架现成 |
| D2 | 版本 | DTS Wiki **9.2.x**（生成时取"首发满 30 天的最新次版本线的最新补丁"，2026-09-29 之后即为 9.3.x）；Spring Boot 由 DTS Wiki 管理（9.2 = 3.5.15，**不单独升级到 Boot 4**）；**Java 25**（DTS Wiki 9.2 兼容列表含 25）；前端 React 19 + **antd 6** + React Router 8 + TanStack Query 5 + Vite + TypeScript（各取 R-012 规则下的最新稳定补丁）；Node 24 LTS；PostgreSQL 18 | 遵循 DTS 版本原则 R-012；Boot 4 等 DTS Wiki 官方支持后随生成器升级。注：dts-stack 现有前端为 antd 5，本项目按规则直接用 antd 6 |
| D3 | 身份 | `authenticationType: oauth2`（服务端会话 + OIDC 授权码），IdP = 现网 Keycloak（realm `yuzhicloud`，client `dts-wiki`） | DTS Wiki oauth2 单体即服务端会话登录；与 Jira、现网 wiki 共用账号 |
| D4 | 权限 | 只到**产品空间**一级：Keycloak client 角色 `reader`/`editor`/`admin`/`space-<slug>` → 应用内权限 `ROLE_USER`/`ROLE_EDITOR`/`ROLE_ADMIN`/`ROLE_SPACE_<SLUG>` | 与现网 wiki 权限模型相同，迁移零成本；需求方明确降低安全优先级 |
| D5 | 事实源 | PostgreSQL 18（独立实例 `wiki-db`）存页面树、全部版本、附件元数据、评论、同步状态 | 需求方决定 |
| D6 | 内容格式 | **Markdown 原文**（与 git 文件字节一致） | 双向同步无损，避免富文本↔Markdown 转换引起伪冲突 |
| D7 | 与 git 的关系 | 页面分三类：`GIT`（同步根下的 .md，双向同步）、`FOLDER`（目录；有 README.md 时即其正文）、`NATIVE`（仅 PG，如会议纪要） | 研发文档与 AI/开发流程共享同一份文件；非研发内容不污染仓库 |
| D8 | 同步实现 | 服务端持有各仓库工作副本，调用 **git CLI**（容器内安装 git + openssh）；出站用 outbox 表，入站轮询 fetch；单实例执行（ShedLock） | 现网 wiki 已验证的做法；JGit 对 rebase/SSH 支持不如 CLI 稳定 |
| D9 | 中文检索 | PG 内实现：`pg_bigm`（首选）或 `zhparser`，由 F0 spike 定；**DTS Wiki `searchEngine: no`** | 不引入 Elasticsearch 集群 |
| D10 | 前端 | **React + antd**（需求方 2026-09-26 指定），独立工程 `frontend/`：antd 组件与主题（主色 `#2f6f5e`，支持暗色）、TanStack Query 管服务端状态、React Router、react-i18next（默认 zh-cn）；管理后台（空间、同步）也用 antd 实现，不使用 DTS Wiki 生成的前端 | 与 DTS 平台前端（React + antd）统一；避免 Bootstrap 与 antd 混用 |
| D11 | 编辑器 | Markdown 原生的所见即所得编辑器：Milkdown 或 Vditor，F0 spike 按"打开-不改-保存字节不变"定 | 保证 D6 |
| D12 | 附件 | 宿主机卷 `/data/dts-wiki-v2/attachments`（按 sha256 去重），经 `BlobStore` 接口访问，日后换 S3（SeaweedFS） | 先简后繁 |
| D14 | 前端构建与托管 | `frontend-maven-plugin` 在 `./mvnw -Pprod package` 时安装 Node、执行 `pnpm install && pnpm build`，产物复制到 `target/classes/static`；后端 `SpaForwardController` 把非 API 路由转发到 `index.html`；开发期 Vite dev server 代理 `/api`、`/oauth2`、`/login` 到 8080 | 保持单 jar 部署；前后端同源，会话 Cookie 无跨域问题 |
| D13 | 部署 | 10.20.0.50 上 docker compose：`wiki-app`（jar，含前端）+ `wiki-db`；验收期内网 `http://10.20.0.50:18091`；上线时把阿里云 `wiki.yuzhicloud.com` 的 `proxy_pass` 改指 18091 | 不新增域名；切换与回退各改一行 |

## 3. 文档索引

| 文档 | 内容 |
|------|------|
| `01-系统架构.md` | 组件、请求与登录流程、部署拓扑 |
| `02-领域模型.md` + dts-wiki 仓库 `wiki-company/dts-wiki.jdl` | 实体、关系、约束；DTS Wiki 生成输入 |
| `03-后端设计.md` | 包结构、服务、REST API、权限、事务、定时任务、Liquibase 约定 |
| `04-git同步设计.md` | 同步模型、状态机、入站/出站算法、冲突与合并 |
| `05-前端设计.md` | 路由、页面、组件、状态管理、编辑器与渲染 |
| `06-部署与运维.md` | 镜像、compose、Keycloak 配置、备份、切换与回退 |
| `07-测试与验收.md` | 测试分层、权限矩阵、同步场景、竖线验收脚本、非功能预算 |
| `08-编码任务与交接说明.md` | 给编码会话的工作顺序、约束、完成定义 |

## 4. 术语

| 术语 | 含义 |
|------|------|
| 空间（Space） | 一个产品的全部内容，如 `dts`（DTS 平台）、`prs`（PRS 花卉租赁） |
| 同步根（SyncRoot） | 空间绑定的 git 仓库中的一个目录，如 dts-rdc 的 `worklog`，挂载到空间内某一节点下 |
| git 绑定页 | `kind=GIT`，对应同步根内一个 `.md` 文件（`gitPath` 为仓库内相对路径） |
| 版本（PageVersion） | 页面的一次完整内容快照；来源 WEB / GIT / MERGE / RESTORE |
| 同步点 | 某同步根最后一次与 wiki 一致的 git 提交（`lastSyncedCommit`） |
