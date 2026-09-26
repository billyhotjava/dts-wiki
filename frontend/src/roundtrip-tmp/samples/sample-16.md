---
sourcePath: "worklog/v1.0.0/sprint-5-202610/features/F6-Finance证明链去领域化/T02-证明引擎契约与proof-suite-schema.md"
---

::: v-pre
# T02: 证明引擎契约与 proof-suite schema

**优先级**: P0
**状态**: DRAFT
**依赖**: T01

## 目标
钉死 `ProofEngine` 的 Java SPI、`proof-suite.v1` JSON Schema 以及结果表结构，保证 T01 中每一种 target 都能被表达。

## 技术设计
- **Java 契约**（`engine-ai/.../service/proof/`）：
  ```java
  public interface ProofSource { String type(); ProofValue fetch(SourceSpec spec, ProofContext ctx); }
  public interface ProofCheck  { String type(); CaseOutcome compare(ProofValue left, ProofValue right, CheckSpec spec); }
  public interface ScorecardPublisher { void publish(ProofRun run, ScorecardSpec spec); }
  public record ProofValue(List&lt;String> keys, List&lt;Map&lt;String,Object>> rows, Map&lt;String,Object> meta) {}
  public record CaseOutcome(Status status, BigDecimal diff, Map&lt;String,Object> evidence) { enum Status {PASS, FAIL, ERROR, SKIPPED} }
  ```
  内置实现：Source：`sql`（必须经过 QueryGateway，F10）、`http-json`（仅限允许清单中的 serviceRef）、`snapshot`（读取上一次 run 的结果）；Check：`equals`、`tolerance`（abs/rel）、`invariant`（单边断言表达式，用 SpEL 的安全子集或自研的小型表达式）、`tieout`（按 key 分组求和后逐组比对）、`dual-path`（同一指标两条计算路径的比对）。
- **schema 示例**：
  ```yaml
  suite: finance-summary-dual
  domain: finance
  cases:
    - id: rent-net-2026-09
      left:  {source: sql, datasource_ref: prs-mart, sql: "SELECT SUM(\"租金净额全口径\") v FROM public.xycyl_ads_finance_... WHERE month=:m", params: {m: "2026-09"}}
      right: {source: sql, datasource_ref: prs-app-legacy, sql: "...", params: {m: "2026-09"}}
      check: {type: tolerance, abs: 0.01}
      severity: high
  scorecard: {name: finance-reconciliation, weights: {high: 3, medium: 2, low: 1}}
  schedule: "0 30 2 * * *"   # 可选：由引擎的调度器执行（多副本下需要加锁：ShedLock 或 PG advisory lock）
  ```
- **结果表 DDL**：见 F6 README 的契约；索引 `(suite, started_at desc)`；保留策略：每个 suite 保留最近 90 次 run（配置项）。
- **accuracyEvidence 接入**：`Nl2Sql` 回答时，按 `domain` 与指标查找最近一次相关 suite 的结果，映射到 S34 的等级规则（**规则本身不变**，只替换数据来源）。

## 验证
- [ ] 用 T01 表格中每种 target 各写一个 schema 样例，都能通过 schema 校验
- [ ] SPI 评审通过（至少一位原 S33 作者参与）

## Definition of Done
- [ ] schema 进入 `dts-studio/protocol/assets/proof-suite.v1.schema.json`；Java 接口合入

:::
