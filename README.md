# DTS Wiki

Confluence 式内部知识协作平台（DTS 知识中心雏形）。

- 架构：JHipster 9 单体后端（skipClient）+ `frontend/` React + antd 前端（打进同一个 jar），Keycloak 统一登录，PostgreSQL 为唯一事实源
- 研发文档：与各产品 git 仓库的 `docs/`、`worklog/` 双向同步（≤ 60 s），冲突进入人工三方合并
- 权限：到产品空间一级

| 文档 | 说明 |
|------|------|
| [docs/design/00-概述与架构决策.md](docs/design/00-概述与架构决策.md) | 目标、非目标、架构决策 |
| [docs/design/01-系统架构.md](docs/design/01-系统架构.md) | 组件、登录与请求流程 |
| [docs/design/02-领域模型.md](docs/design/02-领域模型.md) · [jhipster/dts-wiki.jdl](jhipster/dts-wiki.jdl) | 实体、不变量、自定义 Liquibase |
| [docs/design/03-后端设计.md](docs/design/03-后端设计.md) | 包结构、权限、REST API、事务、定时任务 |
| [docs/design/04-git同步设计.md](docs/design/04-git同步设计.md) | 同步模型、状态机、入站/出站、冲突 |
| [docs/design/05-前端设计.md](docs/design/05-前端设计.md) | 路由、布局、编辑器、渲染 |
| [docs/design/06-部署与运维.md](docs/design/06-部署与运维.md) | 镜像、compose、Keycloak、备份、切换回退 |
| [docs/design/07-测试与验收.md](docs/design/07-测试与验收.md) | 测试分层、权限矩阵、同步场景、验收脚本 |
| [docs/design/08-编码任务与交接说明.md](docs/design/08-编码任务与交接说明.md) | 编码顺序、硬性约束、约定 |

规划与进度：dts-rdc 仓库 `worklog/v1.0.0/sprint-6-202610/`。
