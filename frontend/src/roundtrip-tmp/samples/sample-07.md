# Sprint-1: dts-infra bootstrap

> 2026-09-26 复核：本目录及其 Feature/Task 状态均为 3 月规划快照，不是可执行 READY。以后续 [Sprint-5](../sprint-5-202610/README.md) 及其 [承接映射](../sprint-5-202610/assets/planning-reconciliation-20260926.md) 为准；未承接项留待后续重排。

**时间**: 2026-04
**状态**: SUPERSEDED（历史规划，未执行，退出活动队列）
**目标**: 从零搭建 dts-infra 仓库，实现 bootstrap 工具，完成"从空集群到 commander 运行"的全链路

## 背景

dts-infra 是 DTS 平台的独立基础设施模块，负责整个生态（dts-studio / dts-stack / app-stack）的安装部署和运维。
Sprint-1 聚焦 bootstrap 二进制 — 它是整个平台的第一个入口点：环境预检 → 部署 global-pg → 部署 commander → 交接。
设计文档: `docs/plans/2026-03-26-dts-infra-design.md`

## Feature 列表

| ID | Feature | Task 数 | 状态 |
|----|---------|---------|------|
| F1 | 仓库初始化与项目骨架 | 3 | 历史 READY（停用） |
| F2 | 环境预检 | 2 | 历史 READY（停用） |
| F3 | GlobalPG 部署 | 2 | 历史 READY（停用） |
| F4 | Commander 部署与交接 | 3 | 历史 READY（停用） |
| F5 | Studio 后端骨架 | 5 | 历史 READY（停用） |

## 完成标准
- [ ] dts-infra 仓库结构完整（cmd/bootstrap, internal/bootstrap, proto, .skills, deploy/helm）
- [ ] `dts-bootstrap precheck` 可在目标集群上执行环境预检并输出报告
- [ ] `dts-bootstrap install` 可部署 global-pg StatefulSet 并等待就绪
- [ ] `dts-bootstrap install` 可部署 commander Deployment 并等待健康检查通过
- [ ] bootstrap 退出后 commander 独立运行，gRPC health 可达
- [ ] 全流程在 kind 集群上端到端验证通过
- [ ] Iron Law compliance checks — 所有服务符合五条铁律验证（人工接管/安全/审计/数据安全/API-first）
- [ ] Infra ontology seed data — 基础设施本体种子数据已导入 global-pg

> **注意**: 所有 Task 必须遵循 `.rules/10-architecture/infra-iron-laws.rules`
