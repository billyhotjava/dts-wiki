# Sprint Queue — DTS v1.0.0

## Sprint-1: dts-infra bootstrap (202604)

| Feature | Task 数 | 状态 |
|---------|---------|------|
| F1-仓库初始化与项目骨架 | 3 | SUPERSEDED |
| F2-环境预检 | 2 | SUPERSEDED |
| F3-GlobalPG部署 | 2 | SUPERSEDED |
| F4-Commander部署与交接 | 3 | SUPERSEDED |
| F5-Studio后端骨架 | 5 | SUPERSEDED |

**历史统计**: 原 READY=5；当前活动任务=0（未执行，后续重排）

---

## Sprint-2: dts-infra commander (202605)

| Feature | Task 数 | 状态 |
|---------|---------|------|
| F1-中间件生命周期管理 | TBD | SUPERSEDED |
| F2-Platform组件管理 | TBD | SUPERSEDED |
| F3-健康巡检与告警 | TBD | SUPERSEDED |
| F4-InfraAgent基础能力 | TBD | SUPERSEDED |
| F5-Commander CLI客户端 | TBD | SUPERSEDED |

**历史统计**: 原 READY=5；当前活动任务=0（未执行，后续重排）

---

## Sprint-3: dts-stack 第一版原型 (202606)

| Feature | Task 数 | 状态 |
|---------|---------|------|
| TBD | TBD | SUPERSEDED |

**历史统计**: 当前活动任务=0（未执行，后续重排）

---

## Sprint-4: app-stack 第一版原型 (202607)

| Feature | Task 数 | 状态 |
|---------|---------|------|
| TBD | TBD | SUPERSEDED |

**历史统计**: 当前活动任务=0（未执行，后续重排）

---

> 注：Sprint-1 ~ Sprint-4 为 2026-03 的规划，均未执行；目标架构调整后由 Sprint-5 取代或重排（2026-09-25）。

## Sprint-5: DTS 四模块合并与边界重整 (202610)

**目录**: `worklog/v1.0.0/sprint-5-202610`
**状态**: DRAFT
**目标**: dts-studio（并入 copilot 引擎）运行时加载 prs-stack 的花卉 AppPack，经统一网关登录后在工作台完成"当前在营项目数"问数：受控出口（只读 + 租户隔离 + AST 校验）、审计进入 Kafka、纯搬迁结果等价，安全/租户/口径变化按明确的新预期验收、头脑中不含花卉硬编码。
**依赖**: copilot S1–S34（引擎与证据链）、prs Sprint-1（F3 Keycloak / F7 骨架 / R-008 / R-010）、stack governance 指标（copilot S29 联邦）。

| Feature | 优先级 | 波次 | Task 数 | 状态 |
|---------|--------|------|---------|------|
| F0-G0基线与权威源确认 | P0 | A | 5 | DRAFT(4)/DONE(1) |
| F1-仓库落位与submodule重整 | P0 | A | 6 | READY(3)/DRAFT(3) |
| F2-架构决策定稿与规则体系修订 | P0 | A/B | 7 | DRAFT |
| F3-copilot并入dts-studio | P0 | A | 6 | DRAFT |
| F4-AppPack协议落地与头脑Pack运行时 | P0 | B | 7 | DRAFT |
| F5-花卉领域资产外置为prs-pack | P0 | B | 7 | DRAFT |
| F6-Finance证明链去领域化 | P1 | C | 6 | DRAFT |
| F7-BI收敛-copilot-analytics并回stack | P1 | A/C | 6 | DRAFT |
| F8-口径与指标单一事实源 | P1 | B | 5 | DRAFT |
| F9-统一身份网关与租户上下文 | P0 | B | 6 | DRAFT |
| F10-数据出口安全-受控查询网关 | P0 | B | 7 | DRAFT |
| F11-审计统一入Kafka | P1 | C | 5 | DRAFT |
| F12-DAP协议代码化与AgentUI契约 | P1 | B/C | 5 | DRAFT |
| F13-端到端竖线验收发布与运维 | P0 | A/C | 5 | DRAFT |

**统计**: Feature 14 / Task 83；READY=3, DRAFT=79, IN_PROGRESS=0, DONE=1, BLOCKED=0（DONE 仅 F0/T05 资料接收）
**执行顺序**: 以 Sprint README 的 Task 依赖/波次为准；F7/T01 与 F13/T01 预算定义提前到 A，F2/T06 细则在 F4/T01 后收口，F12/T04 随 F11 在 C 完成。
**关键决策**: ADR-1..4 已定（rdc 总纲 / stack 湖仓 / studio+copilot 合并 / prs 进入 app-stack）；ADR-5..12 提议中；F2 负责 5..10，F3/T02 负责 11，F4/T01 负责 12（推荐：Java 头脑、BI 归 stack、口径 SoT 归 stack、Traefik+forwardAuth 网关、QueryGateway 出口）。
**已知风险**: stack 权威仓库不明（账本#2/#3）；prs Sprint-1 进行中，需要避免干扰；F6 工作量大，可能溢出到 Sprint-6；copilot 旧部署的外部依赖方（老 rs-gateway、prs F7/T04）需要在切换时协调。

**本轮资料承接**：[复核记录](sprint-5-202610/assets/planning-reconciliation-20260926.md)；PRS 93 来源文件已进本地工作副本，远端提交/gitlink 和新 BOM 运行验收仍待对应 Task。

---

## Sprint-6: DTS Wiki v1 —— PG 事实源 + 产品 git 双向同步 (202610)

**目录**: `worklog/v1.0.0/sprint-6-202610`
**状态**: DRAFT
**目标**: 以 PostgreSQL 为唯一事实源的 Confluence 式知识协作平台；各产品 git 仓库的 docs/、worklog/ 与 wiki 双向同步（1 分钟内），冲突不静默覆盖；权限只到产品级。
**依赖**: 现网 wiki（dts-rdc wiki/、deploy/wiki/）的同步与权限经验；Keycloak realm yuzhicloud；与 Sprint-5 并行。

| Feature | 优先级 | Task 数 | 状态 |
|---------|--------|---------|------|
| F0-基线与技术选型spike | P0 | 4 | DRAFT |
| F1-仓库数据模型与应用骨架 | P0 | 4 | DRAFT |
| F2-统一登录与产品级权限 | P0 | 3 | DRAFT |
| F3-空间与页面管理 | P0 | 5 | DRAFT |
| F4-编辑器附件与模板 | P0 | 4 | DRAFT |
| F5-Git双向同步 | P0 | 6 | DRAFT |
| F6-版本历史与追溯 | P0 | 3 | DRAFT |
| F7-搜索与导航 | P1 | 3 | DRAFT |
| F8-协作评论提及通知 | P1 | 3 | DRAFT |
| F9-部署迁移切换与运维 | P0 | 4 | DRAFT |

**统计**: Feature 10 / Task 39；READY=0, DRAFT=39, IN_PROGRESS=0, DONE=0, BLOCKED=0
**执行顺序**: F0 → F1 → F2 → {F3, F4} → F5 → F6 → F7 ∥ F8 → F9
**关键决策**: PG 为事实源；Java 25 + Spring Boot 4 + React 19；内容为 Markdown 原文（无损同步）；新仓库 dts-wiki；权限仅到产品级。
**已知风险**: 编辑器 Markdown roundtrip 可能产生伪变更（F0/T02）；中文检索需自定义 PG 镜像（F0/T03）；各仓库 deploy key 需用户在 GitHub 添加；.50 docker pull 不可用需传镜像。
