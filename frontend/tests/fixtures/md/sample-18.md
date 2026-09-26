---
sourcePath: "worklog/v1.0.0/sprint-5-202610/features/F0-G0基线与权威源确认/T02-冻结合并前copilot行为基线.md"
---

::: v-pre
# T02: 冻结合并前 copilot 行为基线（golden set 快照）

**优先级**: P0
**状态**: DRAFT
**依赖**: T01（基准 SHA）

## 目标
用 copilot 现有的 NL2SQL golden set + S34 accuracyEvidence，在基准 SHA 上跑一次完整快照，
形成 `assets/baseline-golden-answers.{md,jsonl}`，作为 F3/F4/F5/F6/F10/F13 的**回归判据**。

## 技术设计
- **输入**：`AI/resources/governance/nl2sql-accuracy-golden-set.v1.json`（账本#13）；
  现有服务 `Nl2SqlAccuracyGoldenSetScorecardService`、`Nl2SqlAccuracyGoldenSetReleaseEvidenceService`（`AIJ/service/copilot/`）；
  analytics 侧的 `Nl2SqlEvalResource`（账本#22）。
- **输出契约**（jsonl，每行一题）：
  ```json
  {"id":"gs-001","question":"当前在营项目数","domain":"project-fulfillment",
   "route":"template|nl2sql|direct","sql":"...","sql_hash":"sha256",
   "answer":{"columns":["在营项目数"],"rows":[[128]]},
   "evidence_level":"HIGH|MEDIUM|LOW|UNTRUSTED","latency_ms":1830,
   "tenant_id":"1","user_id":"&lt;alice id>","dataset_snapshot":"&lt;id>",
   "metric_version":"&lt;version>","pack_version":"&lt;version>","comparison_class":"move|approved-change",
   "commit_sha":"&lt;copilot sha>","llm":{"provider":"deepseek","model":"..."},"run_at":"2026-10-xxT..+08:00"}
  ```
- **步骤**：
  1. 在基准 SHA 上启动 copilot（沿用 `CP/dev.sh` 或 compose，见 T03）。
  2. 编写 `scripts/baseline/run-golden.sh`（放在 RDC `worklog/v1.0.0/sprint-5-202610/assets/scripts/`，不进入产品仓库）：
     读取 golden set → 依次 `POST /api/ai/agent/chat/send`（或 golden set 评测端点，二选一，优先已有评测端点）→ 解析响应 → 写 jsonl。
  3. **追加主竖线题目**："当前在营项目数""各项目经理负责的在营项目数"（与 prs F7/T04 对齐，账本#28）。
  4. 固定数据快照、用户/租户、模型参数和指标/Pack 版本连跑两次，分别计算规范化 SQL hash 差异与结果语义差异，写进 md 汇总；LLM 温度按生产配置记录。
  5. 汇总表：按 domain 统计题数、路由分布、证据等级分布、P50/P95 时延。
- **错误路径**：LLM 不可用 → 记录为 BLOCKED 并附配置；个别题超时 → 标记 `timeout`，不从基线剔除。

## 影响范围
新增 `assets/baseline-golden-answers.{md,jsonl}`、`assets/scripts/run-golden.sh`。

## 验证
- [ ] jsonl 行数 = golden set 题数 + 2
- [ ] 两次重跑差异率 ≤ 2%（超出则记录不稳定题清单，作为已知噪声）
- [ ] md 汇总中每个数字都能从 jsonl 重算

## Definition of Done
- [ ] 基线文件入库（RDC worklog），脚本可一键重跑
- [ ] Sprint README Gate "合并前行为基线" 置为 PASS

## 2026-09-26 承接约束

两类基线分别判断：纯搬迁在相同上下文要求业务结果等价；租户隔离、安全拦截、口径修正属于明确接受的行为变化，逐题记录旧值/新预期/原因/对应 ADR 或 Task，并按新 oracle 断言。2% 只用于记录 LLM 重跑稳定性，不容忍越权或指标错误；SQL hash 变化本身不等于业务回归。旧 copilot 无租户语义的答案不可作为双租户安全验收的预期。

:::
