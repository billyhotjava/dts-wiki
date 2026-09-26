---
sourcePath: "worklog/v1.0.0/sprint-5-202610/features/F2-架构决策定稿与规则体系修订/T04-ADR008-009统一网关身份与数据出口.md"
---

::: v-pre
# T04: ADR-008/009 统一网关、身份与数据出口（含湖仓底座路线）

**优先级**: P0
**状态**: DRAFT
**依赖**: F0/T03（确认 Keycloak 实例现状，Q4）

## 目标
定稿两件互相关联的事：
（1）dts-gateway 的具体实现和身份传递契约；
（2）头脑查询数据的唯一出口 QueryGateway 的形态，以及 Trino/Ranger/Iceberg 湖仓底座的引入路线（回答 Q3）。

## 技术设计
### ADR-008 统一网关与身份
- **现状**：三个入口（账本#18、#21、#24、#27）。
- **推荐**：Traefik 作为边缘；forwardAuth 指向平台鉴权服务 `dts-auth`（由 prs-auth 提升为平台服务，代码迁到 stack 或 studio 待定，推荐 stack 的 `dts-session-core` 旁边）；Keycloak 单实例，每个客户一个 realm（花卉：`flower` / `flower-test`），租户 = Keycloak Organization。
- **身份头契约（唯一版本）**：`X-DTS-User-Id`(string)、`X-DTS-User-Name`、`X-DTS-Display-Name`(URL 编码)、`X-DTS-Roles`(逗号分隔)、`X-DTS-Tenant-Id`(string，bigint 的字符串形式)、`X-DTS-Dept`、`X-DTS-Trace-Id`、`X-DTS-Service`（机器调用方）。
  下游**只信任网关注入的头**；网关必须先剥离客户端自带的同名头（Traefik `headers.customRequestHeaders` 置空，再由 forwardAuth 回注）。
- **机器间调用**：API Key 仅用于 `X-DTS-Service` 场景，不允许携带用户身份头（修复账本#18 的自报身份问题）。
### ADR-009 数据出口与湖仓路线
- **QueryGateway 契约（接口级，由 F10 实现）**：
  ```java
  QueryResult execute(QueryContext ctx, DatasourceRef ds, String sql, QueryLimits limits);
  record QueryContext(String tenantId, String userId, Set&lt;String> roles, String traceId, String purpose);
  record QueryLimits(int maxRows, Duration timeout, long maxBytes);
  ```
  实现 v1：`JdbcGuardedQueryGateway`（AST 校验 + 只读账号 + READ ONLY 事务 + 租户注入）；实现 v2（路线）：`TrinoRangerQueryGateway`。
- **湖仓路线**：列出 3 个选项及触发条件——①维持 PG + dbt（花卉体量足够）；②引入 Trino 做联邦查询（多源时触发）；③Iceberg + 对象存储（数据量或多引擎需求触发）。写明本 sprint 只做 ①，并记录外部 Hive（账本#24 `HiveDriver`）属于哪个客户场景。
- **产出**：`docs/adr/ADR-008-gateway-identity.md`、`docs/adr/ADR-009-data-egress-lakehouse.md`。

## 验证
- [ ] 身份头契约与 prs `UserContextFilter`、copilot `UserContextFilter` 做字段对照（账本#18、#27），差异逐项给出处理方式
- [ ] 用户签字

## Definition of Done
- [ ] 两份 ADR Accepted；F9、F10 可以推进

:::
