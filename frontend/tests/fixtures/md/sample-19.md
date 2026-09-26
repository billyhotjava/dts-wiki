---
sourcePath: "worklog/v1.0.0/sprint-5-202610/README.md"
---

::: v-pre
# Sprint-5: DTS 四模块合并与边界重整（Studio 头脑 · Stack 湖仓 · PRS App）

**时间**: 2026-10（时间盒假设：2026-10-08 至 2026-12-25，分三个波次，见 §波次计划；国庆后启动，日期待确认后转正）
**状态**: DRAFT
**类型**: Architecture / Repository Consolidation / Security Hardening
**目标**: 在 dts-rdc 一个总纲仓库下，**dts-studio（已并入 copilot 引擎）运行时加载 prs-stack 提供的花卉 AppPack**，
用户经统一网关登录后在 Studio 智能体工作台问"当前在营项目数"，查询经受控出口（只读 + 租户隔离 + AST 校验）执行，
审计事件进入 Kafka；纯搬迁结果与基线等价，租户/安全/口径修正按新预期验收，头脑运行逻辑和可加载资产中不再包含花卉硬编码。

**2026-09-26 修订**：[整体复核及 PRS 基础承接](assets/planning-reconciliation-20260926.md)。旧 Sprint-1～4 退出活动队列；原型/资料接收 F0/T05 已完成，运行 Gate 仍待执行。

## 背景与价值

DTS 以 Palantir 为参照：**dts-stack ≈ Foundry 数据层（湖仓一体中台）**、**dts-studio ≈ Ontology + AIP（AI 头脑：智能体、chat2sql、本体）**、
**dts-app-stack ≈ 面向行业客户的应用（花卉租赁 prs 为首个 App）**、**dts-infra ≈ Apollo（安装运维）**。
dts-rdc 是总纲仓库（定位已确认不变）。

9 月 25 日初查时真实引擎主要在 `/opt/prod/prs/source`，RDC 顶层模块以骨架为主；9 月 26 日补查确认嵌套 prs-stack 已有 3 月前后端原型，且已接收 9 月重写底座（账本#32～35）；
dts-copilot 经 34 个 sprint 已成为事实上的头脑，但它是"三合一"——**通用 AI 引擎 + 花卉领域资产 + 一份与 stack 分叉的 BI**；
原始 dts-prs 没有独立 git；已增量接收进 prs-stack 工作副本，正式提交与新 AppPack 协议仍待 F1/F4/F5；五条铁律中 #2 统一网关、#3 数据出口安全、#4 审计 均未在代码层兑现。

**不做的代价**：
- 头脑持续沉积花卉专用代码（Finance* 已 52 类 / 7.7k 行），第二个行业 App（metro）无法复用；
- BI 两份分叉越走越远，合并成本线性上升；
- copilot 连接 prs 新库前若不解决租户与只读，会把 prs 精心设计的 RLS（R-010）在 AI 出口处击穿；
- prs Sprint-1（2026-09-21 至 10-16）正在定义契约，现在纠正 AppPack 定位成本最低，Sprint-2 后成本倍增。

## 架构决策记录 (ADR)

| # | 决策点 | 选择 | 状态 | 理由 | 影响 |
|---|--------|------|------|------|------|
| ADR-1 | dts-rdc 定位 | 总纲仓库：愿景、铁律、DAP 协议、跨模块 sprint、submodule 索引 | **已定**（用户 2026-09-25） | 统一产品叙事与跨模块治理 | 本 sprint 的 worklog 落在 dts-rdc |
| ADR-2 | dts-stack 定位 | 湖仓一体数据中台（入湖/ODS/dbt/治理/指标/查询/BI） | **已定** | 与 Foundry 数据层对标 | BI 与口径 SoT 倾向归 stack（ADR-6/7） |
| ADR-3 | 头脑 | dts-studio 与 dts-copilot **合并**为 AI 头脑（智能体、chat2sql、本体、Pack 运行时） | **已定** | studio 有规则/协议，copilot 有可运行引擎 | F3/F4 |
| ADR-4 | 花卉租赁 | dts-prs 并入 `dts-app-stack/prs-stack`，作为 DTS 面向花卉租赁的 App | **已定** | App 与平台解耦，验证 AppPack 协议 | F1/F5 |
| ADR-5 | 头脑实现语言 | **Java 为主体**（沿用 copilot），Python 仅作可选 sidecar（eval/embedding），修订 studio 规则中 "Python LangGraph 10 服务" 的设计 | 提议（F2/T01 定稿） | 34 个 sprint 资产不推倒；团队 Java 为主 | `.rules/10-architecture/*` 需改 |
| ADR-6 | BI 归属 | BI（Card/Dashboard/Screen/Report）**归 stack**，头脑保留对话/智能体 UI，通过 API 让 stack 生成 BI 资产 | 提议（F2/T02） | BI 是湖仓消费层；消除分叉 | F7 |
| ADR-7 | 口径/指标 SoT | **stack governance Indicator 为唯一事实源**，头脑只读消费（缓存 + 降级） | 提议（F2/T03） | 消除双事实源；S29 联邦已铺路 | F8 |
| ADR-8 | 统一网关 | **Traefik + forwardAuth 作为 dts-gateway 的实现**，鉴权服务由 prs-auth 演进为平台 `dts-auth`；Keycloak 单实例，Organization=租户 | 提议（F2/T04） | prs 已验证（F3）；stack/copilot 也用 Traefik | F9 |
| ADR-9 | 数据出口 | 头脑所有 SQL 经 **QueryGateway**：AST 白名单 + 只读账号 + READ ONLY 事务 + statement_timeout + 租户上下文注入；Trino/Ranger 作为后续替换实现 | 提议（F2/T04） | 铁律 3；当前 stack 无 Trino/Ranger 运行 | F10 |
| ADR-10 | 版本基线 | 平台（studio/stack）目标对齐 prs R-012（JDK 25 / Boot 4.x），本 sprint 只做评估与路线，不强制升级 | 提议（F2/T05） | 避免合并与升级叠加风险 | F2/T05 spike |
| ADR-11 | 历史保留 | copilot 并入 studio **保留 git 历史**（filter-repo 重写路径到 `engine/` 后 merge `--allow-unrelated-histories`） | 提议（F3/T02 演练后定稿） | 34 sprint 的 blame/证据可追溯 | F3 |
| ADR-12 | 领域资产载体 | 花卉语义包/治理规则/模板/提示词/评测集 以 **AppPack 资产**形式存放于 prs-stack，头脑运行时从 **Pack 注册表**加载，classpath 仅保留过渡回退 | 提议（F4/T01） | 兑现 apppack-protocol.rules | F4/F5/F6 |

## 端到端契约链 (Vertical Slice)

主竖线：alice（租户 ID `1`，显示别名 t1，项目经理）在 Studio 智能体工作台问“当前在营项目数”。

| 层 | 契约/落点 | 签名要点 |
|----|-----------|----------|
| UI 入口 | Studio webapp `AgentWorkspacePage`（路由 `/workspace`，沿用 copilot S27 agent-first 单入口，账本#20） | 输入框提交问题；答案卡片展示数值 + `accuracyEvidence` 等级 + 数据来源 + 审计号 |
| 登录（目标，现状见 PRS-G01） | Keycloak（单实例；realm `flower-test`，client `dts-studio-web`，OIDC PKCE） | access_token 含 `organization`（租户）、`realm_access.roles` |
| 网关 | Traefik（dts-gateway）→ forwardAuth `GET /api/internal/auth/forward`（账本#25） | 2xx 回注 `X-DTS-User-Id / X-DTS-User-Name / X-DTS-Roles / X-DTS-Tenant-Id / X-DTS-Trace-Id`；401/403 直接返回 |
| API | `POST /api/ai/agent/chat/send`（既有，账本#17 所在服务） | req `{sessionId?, message, domainHint?}`；resp copilot 契约 `{responseKind, blocks[], accuracyEvidence{level,reasons[]}, sourceRefs[], auditId}` |
| Service | `IntentRouterService` → `SemanticPackService`（改为 `PackRegistry` 供数，pack `prs-flower@1.0.0` 域 `project-fulfillment`）→ `Nl2SqlService` → `SqlGuard`（AST）→ `QueryGateway.execute(ctx, datasourceRef, sql, params, limits)` | ctx 含 tenantId/userId/roles/traceId；无 tenantId 时 fail-closed |
| 数据 | 首选：stack 兼容视图（prs F7/T03 产物）；备选：prs PG `prs.project`（RLS FORCE） | 只读账号 `dts_brain_ro`；事务 `SET TRANSACTION READ ONLY` + `SET LOCAL statement_timeout='30s'` + `SET LOCAL app.tenant_id=<t>` |
| 审计 | Kafka topic `dts.audit.v1`（CloudEvents 1.0，type `dts.ai.query.executed`） | 字段 `id,source,type,time,subject,tenantid,actorid,actortype,traceid,data{sql_hash,datasource,rows,evidence_level}` |
| 迁移 | studio engine Liquibase `v1_1_0_001__pack_registry.xml`；prs-stack `pack/pack-manifest.yaml` | 表 `studio_pack`, `studio_pack_version`, `studio_pack_asset`（见 F4 契约） |

## 现状勘察账本 (Context Ledger)

> #1～31 为 2026-09-25 的历史快照，#32 起追加 2026-09-26 的定向核查。优先复用已有证据；发生导入/提交/部署变化时核验受影响项，不重复全量勘察。旧条目不是当前运行状态证明。
> 路径简写：`PRS=/opt/prod/prs/source`，`RDC=/opt/prod/dts/dts-rdc`，`CP=PRS/dts-copilot`，`AI=CP/dts-copilot-ai/src/main`，`AIJ=AI/java/com/yuzhi/dts/copilot/ai`。

| # | 事实 | 证据 |
|---|------|------|
| 1 | RDC 4 个 submodule 均为空壳（仅 README）；`dts-stack` 指向 `b2a674b`（2026-03-27 "first commit"），URL `git@github.com:billyhotjava/dts-stack.git` | `RDC/.gitmodules`；`git -C RDC/dts-stack log` |
| 2 | `PRS/dts-stack` 与 RDC 的 dts-stack submodule **同一 URL 但历史不相交**（`b2a674b` 不在其历史中）；`PRS/dts-stack` 2877 commits，根提交 `7d35cfb73 init`，有 5 个未提交修改（docker-compose-app.yml / docker-compose.dev.yml / imgversion.conf / init.sh / services/dts-pg/init/10-init-users.sh） | `git -C PRS/dts-stack` |
| 3 | `/opt/prod/s10/v2.2.3` 是另一仓库 `billyhotjava/s10-stack.git`（3020 commits，HEAD `8568eb95d` 不在 PRS/dts-stack 中）；stack 的 CLAUDE.md 规定开发目录 `/opt/prod/s10/v2.2.3`、构建目录 `/data/dts-stack` | `PRS/dts-stack/CLAUDE.md` 第 3–4 行 |
| 4 | `RDC/dts-app-stack/.gitmodules` 注册 `metro-stack`、`prs-stack`（`billyhotjava/prs-stack.git` @ `fc3d0e7`），均未 checkout | `git -C RDC/dts-app-stack submodule status` |
| 5 | `RDC/dts-studio/.gitmodules` 又嵌套了 `dts-stack`、`app-stack` → 与 RDC 形成循环嵌套；studio 内 `dts-stack/`、`app-stack/` 为空目录 | `RDC/dts-studio/.gitmodules` |
| 6 | `RDC/dts-studio/worklog/v1.0.0/evolution/` 与 `RDC/worklog/v1.0.0/evolution/` 内容完全重复（BP/产品说明/定价 md+pptx+pdf+生成脚本） | `find` 对比 |
| 7 | `PRS/dts-prs` **不是 git 仓库**；`sources/*/target/` 编译产物在源码树；`sources/deploy/.env` 含 `PRS_PG_PASSWORD`、`PRS_LEGACY_PASSWORD`；已有 `.env.example` | `ls -la PRS/dts-prs` |
| 8 | `PRS` 根不是 git；`PRS/AGENTS.md` 描述的是老系统 adminapi/adminweb/app（已移入 `PRS/archived/`），已过时 | `PRS/AGENTS.md` |
| 9 | copilot：`billyhotjava/dts-copilot.git` main，150 commits，未提交修改 README.md/build.sh/dev.sh/docker-compose.yml/imgversion.conf；**`.env` 被 git 跟踪**（含 `PG_PASSWORD`、`DTS_DBT_DB_PASSWORD`；`LLM_API_KEY` 为空），最近相关提交 `366d0d2` | `git -C CP ls-files .env` |
| 10 | copilot 模块：ai 302 Java、analytics 296 Java、webapp 365 TS；测试 146 个；Java 21 / Spring Boot 3.4.5 | `CP/pom.xml` |
| 11 | `AIJ/service/copilot/` 87 类 16,895 行；其中 `Finance*` 52 类 7,689 行；`AssetBackedPlannerPolicy.java` 1,578 行、`TemplateMatcherService.java` 881 行、`CopilotChatContract.java` 838 行 | `wc -l` |
| 12 | `SemanticPackService.java:21` 从 **classpath** 加载 `semantic-packs/*.json`（:31 起列举）；6 个包：field-operations / finance / flowerbiz / procurement / project-fulfillment / warehouse，共 58,742 字节；schema 键：`domain, description, objects, links, metrics, signals, actions, synonyms, fewShots, guardrails` | `AI/resources/semantic-packs/` |
| 13 | `AI/resources/governance/` 19 个 JSON（17 个 finance/voucher，另有 `caliber-rules.v1.json`、`caliber-cross-source-regression.v1.json`、`nl2sql-accuracy-golden-set.v1.json`）；由 `CaliberRuleRegistry.java:19`、`FinanceInvariantRegistry.java:20` 等从 classpath 读取；另有 `AI/resources/prompts/{flowerbiz,settlement}-{constraints,few-shots}.txt`、`planner/business-direct-responses.json` | `ls AI/resources/*` |
| 14 | Liquibase `AI/resources/config/liquibase/changelog/` 共 34 个；`010`–`034` 大量为**领域查询模板数据**（flowerbiz/finance/procurement/project/warehouse），含 `026__trino_compatible_flowerbiz_sales_templates.xml` | `ls` |
| 15 | `flowerbiz.json:257-258` action endpoint `{"service":"adminapi","draft":"/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt","commit":".../saveFlowerBadDebt"}`；调用方 `HttpAdminApiActionClient`、`OntologyActionExecutor`、`OntologyActionApprovalService` | grep |
| 16 | 工具：`service/tool/builtin/{ExecuteQueryTool,SchemaLookupTool}`、`service/tool/garden/{FinanceSummaryTool,FlowerStatsTool,GardenProjectQueryTool}`；注册 `ToolRegistry`；连接 `ToolConnectionProvider`/`ManagedToolConnectionProvider` | `ls AIJ/service/tool` |
| 17 | SQL 安全：`SqlSafetyChecker.java:17` 正则关键词黑名单（INSERT/UPDATE/DELETE/DROP/TRUNCATE/ALTER/CREATE/GRANT/REVOKE/EXEC/EXECUTE/MERGE/REPLACE），未覆盖 CALL/DO/COPY/SET/LOCK/SELECT INTO/副作用函数；`ExecuteQueryTool.java:82` 开连接后仅 `setMaxRows`、`setQueryTimeout(30)`，**无只读事务**；copilot 主代码中 `tenant` **零命中**；`setReadOnly`/`READ ONLY` 零命中 | grep |
| 18 | 认证：`AIJ/security/ApiKeyAuthFilter.java:25`（`Bearer cpk_*`）；`UserContextFilter.java:24-28` 读取 `X-DTS-User-Id/User-Name/Display-Name/Roles/Dept`，`:42` 无头时用 API Key 名作为用户 → **持 Key 者可自报任意用户身份**；无 `X-DTS-Tenant-Id` | 源码 |
| 19 | `AI/resources/application.yml:73-80` 已有 `DTS_PLATFORM_BASE_URL / TOKEN_URL / CLIENT_ID / CLIENT_SECRET / SERVICE_TOKEN` 等，用于 S29 平台指标联邦 | 源码 |
| 20 | Agent UI 契约：后端 `CopilotChatContract.java`（838 行，Map 组装）；前端手写于 `CP/dts-copilot-webapp/src/components/copilot/{useCopilotStream,copilotStreamReducer,MessageList,copilotFixedReportMessage}.ts(x)`；**无 JSON Schema、无代码生成**；工作台页 `src/pages/AgentWorkspacePage.tsx` | 源码 |
| 21 | copilot 自带 compose：pgvector `0.8.6-pg18`、ollama `0.18.0`、traefik `v3.7.13`；镜像 `dts-copilot-{ai,analytics,webapp}`；`build.sh` = `mvn clean package -DskipTests && docker compose build` | `CP/docker-compose.yml:3-193`、`CP/build.sh` |
| 22 | BI 分叉：`CP/dts-copilot-analytics/.../web/rest` 58 个 vs `PRS/dts-stack/source/dts-analytics/.../web/rest` 59 个，**同名 50 个**；copilot 独有 8：AnalysisDraft / CopilotAdmin / CopilotChat / EltMonitor / FixedReport / PlatformIndicator / ReportTemplateCatalog / Synonym；stack 独有 9：AnalysisExceptionHandler / Analysis / AnalyticsClassificationMigration / DataPortal / internal / Marketplace / ProjectCockpit / SemanticPublish / Semantic；copilot analytics 含 `resources/metabase` | `comm` |
| 23 | stack 源码模块（Java/TS 文件数）：dts-platform 2251、dts-platform-webapp 1646、dts-analytics 383、dts-admin 312、dts-admin-webapp 291、dts-ingestion 257、dts-metrics 62、dts-common 18、dts-metrics-webapp 14、dts-session-core 5；`dts-analytics-webapp` 0 文件（BI 前端落点待确认，platform-webapp 有 `pages/workbench/components/ScreenStrip.tsx`） | `find` |
| 24 | stack `docker-compose-app.yml` 服务：dts-core/admin/admin-webapp/airflow(init/scheduler/triggerer/webserver)/analytics/dbt/elasticsearch/ingestion/keycloak/openmetadata(+ingestion/init)/kafka/kafka-ui/pg/dbt-runtime-init/platform/platform-webapp/proxy；**无 Trino/Ranger/对象存储服务**；`:1313` `DTS_JDBC_DRIVER_CLASS=org.apache.hive.jdbc.HiveDriver`（外部 Hive）；`services/dts-ranger/` 空；花卉 dbt 产物在 PG `public.xycyl_*` | compose |
| 25 | stack 治理指标体系：`dts-platform/.../service/governance/Indicator*`（定义/派生/发布/证据/查询计划）；stack 内嵌 LLM 调用：platform `service/modeling` 16 文件、`service/governance` 8、analytics 17 文件、admin 7、ingestion 7 | grep |
| 26 | 版本：stack、copilot 均 Java 21 / Boot 3.4.5；stack platform-webapp React 18.3 / antd 5.22；copilot webapp React 19.1 / antd 5.24；prs JDK 25 / Boot 4.1.1（`PRS/dts-prs/sources/pom.xml`） | pom/package.json |
| 27 | prs 骨架：prs-common（R 信封、`UserContextFilter` X-DTS-* :24-27）、prs-platform:8082（Liquibase 基线）、prs-auth:8081（`ForwardAuthController` `/api/internal/auth/forward` :30/:47）、prs-shadow:8083、prs-project:8084（RLS 只读 + 老库同步）；Keycloak realm `flower-test`（`sources/deploy/keycloak/realm-flower-test.json`）；Helm 骨架 `sources/deploy/helm/prs-service` | `PRS/dts-prs/sources/README.md` |
| 28 | prs 决策 R-003..R-012 位于 `PRS/dts-prs/worklog/v1.0.0/sprint-queue.md:25-73`（R-008 绞杀共存、R-009 agent UI 协议归 copilot、R-010 多租户 RLS、R-012 版本原则）；prs worklog **未提及** AppPack / dts-rdc / pack-manifest；prs F7/T04 依赖 copilot 问数对照 | grep |
| 29 | studio 设计：`RDC/worklog/v1.0.0/docs/plans/2026-03-11-ai-decision-os-design.md` §3/§6 规定 Python+LangGraph AI Core 10 服务；`dts-studio/.rules/10-architecture/apppack-protocol.rules` 定义 manifest 10 类能力；`service-boundaries.rules` 规定 data-security 为唯一出口；DAP 文档 `RDC/worklog/v1.0.0/docs/dts-agent-protocol.md`（Ontology:47 / Skill:141 / Intent:232 / Security:309 / Audit:349） | 文档 |
| 30 | copilot worklog 12 MB、34 个 sprint（`CP/worklog/v1.0.0/`），含 `CP/worklog/prs/v1/`（dbt 模型包、ODS DDL）；队列规则"同一时间最多一个 IN_PROGRESS"已被违反（S33 有 8 个 IN_PROGRESS；S9/S15/S16/S24/S25/S31 悬挂） | `CP/worklog/v1.0.0/sprint-queue.md` |
| 31 | stack worklog 中已有 `PRS/dts-stack/worklog/prs/prs-flowerbiz-*.json`（报花域看板/钻取定义），为 stack 侧花卉资产 | `ls` |
| 32 | 已初始化 prs-stack 到既有 `fc3d0e7`（2026-03-09），有 backend/frontend/pack 与 21 个页面文件；不是空仓库 | prs-stack Git HEAD；integration-20260926/prototype-capability-map.md |
| 33 | 原 dts-prs 的 93 个文件已原样接收到 prs-stack/sources 与 worklog；新增资料未提交，父子 gitlink 未更新 | source-manifest.json；F0/T05 |
| 34 | PRS 实际使用 groups、缺租户 default、数字租户、两租户各一个项目；RLS/同步路由与口径有待验证项 | integration-20260926/README.md 的 PRS-G01～G07 |
| 35 | R-012/BOM 为 9 月 18 日，公共镜像快照为 9 月 20 日；历史运行证据属于旧 BOM，新配置仅有静态核对 | integration-20260926/version-handoff.md；新 BOM 仍由 F0/T03 验收 |

**开放问题**（勘察未决，由对应 Task 关闭）：
- Q1 dts-stack 权威仓库：GitHub `dts-stack` 当前内容是哪份？`s10-stack` 与 `dts-stack` 的关系（分叉/改名/客户交付线）？→ F0/T01
- Q2 stack BI 前端落点（`dts-analytics-webapp` 为空）→ F7/T01
- Q3 外部 Hive（`HiveDriver`）对应哪个客户环境？湖仓底座（Iceberg/Trino）是否本期引入？→ F2/T04
- Q4 Keycloak 是否已与 stack 共用单实例（prs R-007 称共用）→ F9/T01
- Q5 stack 升级 Boot 4 的成本 → F2/T05
- Q6 copilot-analytics 生产环境是否有用户数据（dashboard/card/screen）需要迁移 → F7/T01（波次 A 盘点）；F7/T04 执行迁移

## Gate Registry

| Gate | 项目 | 状态 | 证据 | 未过则关联 Task |
|------|------|------|------|-----------------|
| G0 | 交付基线（三系统可同机启动、可登录、可问数） | PENDING | `it/baseline.md` | F0/T03 |
| G0 | 领域与数据画像 | GAP | 本 sprint 为架构重整，领域画像复用 prs `F1 迁移清单` 与 copilot S25/S30；需补 `assets/domain-profile.md` 摘要 | F0/T04 |
| G0 | 领域不变量自检（五条铁律 + domain-dts 包） | GAP | 铁律 #2/#3/#4 未兑现（账本#17/#18/#24） | F9 / F10 / F11 |
| G0 | 合并前行为基线（回归参照） | PENDING | `assets/baseline-golden-answers.md` | F0/T02 |
| G1 | 契约链贯通 | GAP | 本文档 §端到端契约链；QueryGateway/PackRegistry 为新契约，待 F4/F10 钉死 | F4/T01、F10/T02 |
| G1 | 非功能预算 | PENDING | `assets/nfr-budget.md`（问数 P95、Pack 加载时延、审计丢失率） | F13/T01 |
| G3 | 发布安全（旧 copilot 部署并行、可回退） | PENDING | `assets/release-plan.md` | F13/T03 |
| G4 | 可运维性 | PENDING | `assets/runbook.md` | F13/T04 |
| G4 | DoD 验收 | PENDING | `it/` | F13/T02 |

## 波次计划（时间盒假设）

| 波次 | 日期（假设） | Feature | 出口条件 |
|------|--------------|---------|----------|
| A 落位与定案 | 10-08 ~ 10-23 | F0、F1 前置项、F2 原则、F3；F7/T01；F13/T01 预算定义 | 前置仓库结构到位、ADR-005～011 按各 Task 定稿、copilot 以历史保留方式进入 studio 且可构建 |
| B 边界拆分与安全 | 10-26 ~ 11-27 | F4、F5、F8、F9、F10；F2/T06–T07 收口；F12/T01/T02/T03/T05 | 头脑从 Pack 注册表加载花卉资产；统一网关 + 受控出口可用 |
| C 收敛与验收 | 11-30 ~ 12-25 | F6、F7/T02–T06、F11、F12/T04；F13 实测/验收 | Finance 去领域化、BI 单份、审计入 Kafka、竖线验收通过 |

> 与 prs Sprint-1（至 10-16）的协同：本轮已做资料接收 F0/T05；后续 git 落位、R-013/协议同步与 PRS-G01～04 修复需列出对 PRS F3/F4/F7 的影响和验证窗口，不能以“不打断”省略这些依赖。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 波次 | 状态 |
|----|---------|---------|--------|------|------|
| F0 | G0 基线与权威源确认 | 5 | P0 | A | DRAFT（4）/ DONE（T05） |
| F1 | 仓库落位与 submodule 重整 | 6 | P0 | A | READY（T01/T03/T04）/ DRAFT（3） |
| F2 | 架构决策定稿与规则体系修订 | 7 | P0 | A/B | DRAFT |
| F3 | copilot 并入 dts-studio（保留历史） | 6 | P0 | A | DRAFT |
| F4 | AppPack 协议落地与头脑 Pack 运行时 | 7 | P0 | B | DRAFT |
| F5 | 花卉领域资产外置为 prs-pack | 7 | P0 | B | DRAFT |
| F6 | Finance 证明链去领域化（通用证明引擎） | 6 | P1 | C | DRAFT |
| F7 | BI 收敛：copilot-analytics 并回 stack | 6 | P1 | A 调研/C 实施 | DRAFT |
| F8 | 口径与指标单一事实源（stack governance） | 5 | P1 | B | DRAFT |
| F9 | 统一身份、网关与租户上下文 | 6 | P0 | B | DRAFT |
| F10 | 数据出口安全（受控查询网关） | 7 | P0 | B | DRAFT |
| F11 | 审计统一入 Kafka | 5 | P1 | C | DRAFT |
| F12 | DAP 协议代码化与 Agent UI 契约 | 5 | P1 | B/C | DRAFT |
| F13 | 端到端竖线验收、发布与运维 | 5 | P0 | A 定义/C 验证 | DRAFT |

**统计**: Feature 14 / Task 83；READY=3，DRAFT=79，DONE=1（资料接收）。

**依赖顺序**: F0/T01 → F1/T01～T04 前置交付；F7/T01 → F2/T02；F2/T01/T04 → F4/T01 → F2/T06 细则；F3/T03 后完成 F1/T06；{F4 → F5 → F6}、{F9 → F10 → F11} 与 F8/F12/F7 按 Task 并行；全部 → F13
**关键路径**: F0/T01 → F1/T02 → F2/T01 → F3/T03 → F4/T04 → F5/T02 → F10/T03 → F13/T02

## 追溯矩阵 (Traceability)

| 需求点 | Feature | 关键 Task | 验收证据位置 |
|--------|---------|-----------|--------------|
| ADR-1 dts-rdc 总纲，submodule 指向真实仓库 | F1 | F1/T02 | `it/IT-01-repo-layout.md` |
| ADR-3 studio+copilot 合并 | F3 | F3/T03、F3/T04 | `it/IT-02-studio-build.md` |
| ADR-4 prs 进入 app-stack | F1、F5 | F1/T01、F5/T01 | `it/IT-01`、`it/IT-04-pack-install.md` |
| ADR-12 头脑无领域硬编码 | F4、F5、F6 | F4/T04、F5/T07、F6/T05 | `it/IT-05-no-domain-in-engine.md`（静态扫描） |
| ADR-6 BI 单份 | F7 | F7/T03 | `it/IT-07-bi-merge.md` |
| ADR-7 口径 SoT | F8 | F8/T03 | `it/IT-08-caliber-sot.md` |
| 铁律 #2 统一网关 | F9 | F9/T02、F9/T03 | `it/IT-03-gateway.md` |
| 铁律 #3 数据出口 | F10 | F10/T01–T06 | `it/IT-06-query-gateway.md` + 红队用例报告 |
| 铁律 #4 审计 | F11 | F11/T02–T04 | `it/IT-09-audit.md` |
| 主竖线（在营项目数） | F13 | F13/T02 | `it/IT-10-e2e-slice.md` |
| 无回归 | F0、F13 | F0/T02、F13/T02 | `assets/baseline-golden-answers.md` vs `it/IT-10` |

## 完成标准

- [ ] **仓库**：RDC 的 4 个 submodule 均指向真实仓库和真实提交；dts-prs 在 prs-stack 中受版本控制；无循环嵌套；无被跟踪的 `.env`。
- [ ] **契约**：`pack-manifest` v1 JSON Schema、`PackRegistry` REST、`QueryGateway` 接口、`dts.audit.v1` 事件、agent UI 消息 Schema 均有契约测试。
- [ ] **边界**：静态扫描确认 studio engine 源码与 classpath 中无 `flowerbiz|xycyl_|rs-flowers|Finance[A-Z]` 领域硬编码（仅保留经登记的不可变迁移历史例外；运行期 Finance 例外清零）。
- [ ] **安全**：红队 SQL 用例集（≥40 条）全部被拦截或在只读事务中失败；跨租户查询 0 行；无租户上下文时 fail-closed。
- [ ] **UI**：alice 在 Studio 工作台完成提问，看到答案、证据等级与审计号（空/加载/错误/成功四态截图）；Pack 管理页可查看已安装 Pack。
- [ ] **竖线**：在运行实例上完成主竖线，Kafka 中可查到对应审计事件；按 F0/T02 的上下文与变更分类验证，安全/租户错误零容差。
- [ ] **可回退**：旧 copilot 部署在切换窗口内保留，可一键切回。

## 非目标

- 不在本 sprint 引入 k8s 运行时、dts-infra（Go）实现、Neo4j/ClickHouse。
- 不实施 Trino/Ranger/Iceberg 湖仓底座（仅在 QueryGateway 预留实现位，并由 F2/T04 出路线）。
- 不做 JDK 25 / Boot 4 的实际升级（F2/T05 只出评估与路线）。
- 不重写 copilot 的 NL2SQL 算法、不新增业务问答域。
- 不改变 prs Sprint-1 的业务范围与时间盒；不迁移老系统业务功能。
- 不处理 metro-stack。

:::


---

<a class="wiki-new-page" href="/edit?new=1&amp;dir=worklog%2Fv1.0.0%2Fsprint-5-202610" target="_self" data-full-nav>＋ 在此目录新建页面</a>
