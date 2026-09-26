# DTS Wiki

Confluence 式内部知识协作平台（DTS 知识中心雏形）。

- 架构：JHipster 9 单体后端（skipClient）+ `frontend/` React + antd 前端（打进同一个 jar），Keycloak 统一登录，PostgreSQL 为唯一事实源
- 研发文档：与各产品 git 仓库的 `docs/`、`worklog/` 双向同步（≤ 60 s），冲突进入人工三方合并
- 权限：到产品空间一级

设计与规划（开发文档）在 dts-rdc 仓库的 worklog 中：
`worklog/v1.0.0/sprint-6-202610/`（Sprint 规划、Feature/Task、验收证据），概要设计见其 `features/F0-基线与技术选型spike/design/00–08`。
作为 dts-rdc 子模块检出时，相对路径为 `../worklog/v1.0.0/sprint-6-202610/features/F0-基线与技术选型spike/design/`。

本仓库 `docs/` 只放正式文档（产品/用户文档、对外文档、运维 runbook）。

