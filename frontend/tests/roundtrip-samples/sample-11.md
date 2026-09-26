# DTS — AI Decision Operating System

> Decision Twins System: Ontology + Intent + Agent
> 把专家脑中的隐性经验变成系统里能自动运转的能力包
> RDC = Research Development Center (研发中心)

## Five Iron Laws (五条铁律 — 最高优先级)

1. **人工随时可接管** — 关掉 AI 系统照样能用。AI 能力可降级，业务 App 和数据平台独立可用。
2. **核心安全不可绕过** — 所有请求必经 dts-gateway 认证，无旁路直连，AppPack 无特权。
3. **数据安全是基因** — 所有数据出口必经受控安全边界（本期 QueryGateway 方案见 ADR-009；含 AI RAG 检索），AI 只看授权数据。
4. **全操作可追溯** — 人和 AI 的操作 → Kafka → dts-audit-log，append-only 不可篡改。
5. **能力先于界面** — API-first，每个服务先有完整 API + 测试，再做前端。

## Architecture

### 当前规划权威

以 `worklog/v1.0.0/sprint-5-202610/README.md` 及其 ADR 状态为准。3 月设计/旧 Sprint 中的 Python 头脑、25 服务、All-in-K8s 等是历史方案，变更映射见 `assets/planning-reconciliation-20260926.md`；不能在新 Sprint 中同时按两套边界实施。

- **dts-rdc**：总纲、跨模块规划与子仓库索引。
- **dts-studio**：并入 copilot 的 AI 头脑；Java 主体是 ADR-005 提议，未定稿前不宣称完成。
- **dts-stack**：湖仓数据中台；BI/指标归属由 ADR-006/007 定稿。
- **dts-app-stack**：行业业务 App；PRS 首个验证，旧 metro 规划后排。
- **dts-infra**：安装运维，保留设计，本期不实现 k8s/Go infra。

### 版本与工作副本

PRS 输入见 `dts-app-stack/prs-stack/worklog/v1.0.0/integration-20260926/`。BOM/公共镜像是 9 月锁定快照；应用版本与公共镜像分开管理，静态配置不代表升级运行通过。
原始工作副本在 F0/T01 确认、F1/T06 切换前仍沿用各仓库现有开发/构建约束；本次本地资料接收不自动改变权威路径，不在源目录初始化新历史或覆盖旧原型。
当前采用 Compose 验证；通信、安全与部署实现按 Accepted ADR 执行，不能从历史愿景直接推定服务已经运行。

## Rules & Skills

详细规则和技能定义在 `dts-studio/` 仓库中，开发前必须阅读对应层级的规则；其中旧架构条款按 Sprint-5 的已定边界及 ADR 状态解释，具体同步由 F2/T06 承接：

### dts-studio/.rules/ — 工作守则 (23 rule files)
- `00-foundation/` — 五条铁律 + 产品理念 (**HIGHEST priority**)
- `10-architecture/` — 架构原则 + 服务边界 + AppPack 协议 + **Infra 铁律应用**
- `20-development/` — 编码规范 + Git 工作流 + API 设计 + 依赖策略
- `30-testing/` — 测试策略 + 质量门禁 (PR/Nightly/Release)
- `40-deployment/` — K8s 部署运维 + 发布流程
- `50-appstack/` — Pack 开发指南 + 行业规则 (energy/research/manufacturing)
- `60-skills/` — AI 技能设计规范 + 分类体系 + 生命周期
- `90-process/` — worklog 组织规范

### dts-studio/.skills/ — 技能清单 (57 skills, placeholder)
- `00-platform/` — 17 个平台内置技能 (data/ontology/query/governance/report/action)
- `10-data/` — 6 个数据层技能 (connector/quality)
- `20-ai/` — 9 个 AI 核心技能 (agent/eval)
- `30-industry/` — 18 个行业技能 (energy/research/manufacturing)
- `40-devops/` — 7 个运维技能

## Project Structure

```
/opt/prod/dts/dts-rdc/           # RDC = Research Development Center
├── dts-infra/                   # 基础设施平台 (submodule, Go)
├── dts-studio/                 # AI 头脑 (submodule，合并 copilot 规划)
│   ├── .rules/                  # 工作守则 (23 rule files)
│   ├── .skills/                 # 技能清单 (57 skills)
│   └── .memory/                 # 领域知识 (ontology/conversations/decisions)
├── dts-stack/                   # 湖仓数据中台 (submodule)
├── dts-app-stack/               # 行业 App (submodule)
├── worklog/                     # 工作日志 (sprint-workflow 格式)
│   └── v1.0.0/                  # 当前版本工作记录
│       ├── sprint-queue.md      # Sprint 全局队列
│       ├── sprint-{N}-{YYYYMM}/ # Sprint 目录 (Feature > Task > IT)
│       ├── docs/                # 设计文档 / 计划
│       ├── draft/               # 旧版 sprint 归档
│       └── evolution/           # 产品文档
└── CLAUDE.md                    # 本文件 — 项目入口
```

## 文档放置规则

- **与开发有关的文档一律放在 `worklog/`**：设计与方案、技术选型/spike 结论、Sprint/Feature/Task、验收证据（`it/`）、会议决策、非功能预算等，按 sprint-workflow 组织在对应 Sprint 的 Feature/Task 或 `assets/` 下。
- **`docs/` 只放正式文档**：产品文档、用户手册、对外文档、运维 runbook 等可以交付或长期对外引用的内容。
- 子仓库（dts-wiki、prs-stack 等）同样遵守：开发文档放 dts-rdc 的 `worklog/`（或该仓库自己的 `worklog/`），不放在代码仓库的 `docs/`。

## Key References

- Infra design: `worklog/v1.0.0/docs/plans/2026-03-26-dts-infra-design.md`
- Architecture design: `worklog/v1.0.0/docs/plans/2026-03-11-ai-decision-os-design.md`
- Infra iron laws: `dts-studio/.rules/10-architecture/infra-iron-laws.rules`
- Product docs: `~/Documents/dts/` (商业计划书, 产品介绍, Palantir 分析)
- Memory: `~/.claude/projects/-opt-prod-dts-dts-rdc/memory/`

## Working Language

- 与用户交流使用中文
- 代码注释和文档使用英文
- 规则文件使用英文（技术标准化）
