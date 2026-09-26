# DTS RDC

Decision Twins System 的总纲仓库：产品方向、跨模块架构决策、Sprint 工作记录和子仓库索引。

当前工作以 [Sprint-5](worklog/v1.0.0/sprint-5-202610/README.md) 为准。3 月的设计与 Sprint-1～4 保留为历史资料，承接关系见 [规划复核](worklog/v1.0.0/sprint-5-202610/assets/planning-reconciliation-20260926.md)。

## 模块定位

| 模块 | 定位 | 当前边界 |
|---|---|---|
| dts-studio | AI 头脑：智能体、问数、本体、Pack 运行时 | 合并现有 copilot；Java 主体方案仍待 ADR-005 定稿 |
| dts-stack | 湖仓数据中台：接入、治理、指标、查询 | BI 与指标事实源的具体边界待 ADR-006/007 定稿 |
| dts-app-stack | 面向行业的业务 App | PRS 为本轮首个 App，向头脑提供领域资产；metro 后续重排 |
| dts-infra | 安装运维 | 保留规划；本 Sprint 不实施 Go infra 或 k8s 运行时 |

本仓库是 dts-rdc，不是 dts-studio。四模块定位已定；未接受的实现 ADR 不能视为已完成。

## PRS 已有基础

[prs-stack 入口](dts-app-stack/prs-stack/README.md) 保留 3 月 backend/frontend/pack 原型，并接收 `/opt/prod/prs/source/dts-prs` 的 9 月 `sources/` 与 `worklog/`。
[承接记录](dts-app-stack/prs-stack/worklog/v1.0.0/integration-20260926/README.md) 包含来源哈希、能力映射、版本快照和待验证差异。
当前是本地工作副本成果，尚未提交/推送三层仓库；新 BOM 构建、运行与租户安全验收仍待执行。

## 工作入口

- [Sprint 队列](worklog/v1.0.0/sprint-queue.md)
- [Worklog 索引](worklog/v1.0.0/README.md)
- [DAP 历史设计](worklog/v1.0.0/docs/dts-agent-protocol.md)：本期协议代码化由 Sprint-5 F12 承接
- [项目操作约定](CLAUDE.md)

五条铁律持续有效：人工可接管、核心安全不可绕过、数据只对授权主体可见、操作可追溯、能力先于界面。
网关、QueryGateway 与审计的具体实现分别由 F9/F10/F11 验证，不能把设计目标当作现有部署事实。

## License

Proprietary. All rights reserved.
