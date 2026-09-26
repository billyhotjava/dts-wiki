---
sourcePath: "worklog/v1.0.0/docs/plans/2026-03-26-dts-infra-design.md"
---

::: v-pre
# DTS-Infra Design Specification

> 2026-09-26 状态说明：本文保留历史设计/审批记录。模块定位、实施顺序、首个 App 和本期运行方式以 [Sprint-5 承接映射](../../sprint-5-202610/assets/planning-reconciliation-20260926.md) 为准；ADR-5～12 尚待各 Task 定稿，本文的旧选型不能覆盖新 Sprint，也不能视为当前已实现。

**Date**: 2026-03-26 (updated 2026-03-27)
**Status**: DRAFT
**Author**: AI-assisted collaborative design
**Governing Rules**: `.rules/00-foundation/five-iron-laws.rules` (HIGHEST priority)
**Infra-specific Rules**: `.rules/10-architecture/infra-iron-laws.rules`

## 1. Overview

### 1.1 Problem Statement

Current Go infrastructure code (dts-operator + dts-cli) lives inside `dts-stack/infra/`,
tightly coupled to dts-stack's 25 services. As DTS evolves into a multi-module platform
(dts-studio + dts-stack + app-stack), a dedicated infrastructure module is needed to
manage installation, operations, and lifecycle of ALL modules uniformly.

### 1.2 Solution

Extract infrastructure into an independent module **dts-infra** — a complete infrastructure
platform that manages the entire DTS ecosystem with AI-native operations.

### 1.3 Repository Strategy

- **Independent Git repository**: `github.com/billyhotjava/dts-infra`
- **Submodule reference**: included in the main `rdc` repo as a git submodule
  (same pattern as metro-stack / prs-stack)

### 1.4 Relationship to Original Architecture

This design **supersedes** the original Go layer definition in `2026-03-11-ai-decision-os-design.md`
and `.rules/10-architecture/service-boundaries.rules`, which defined two Go services:
`dts-operator` + `dts-cli`.

Mapping:
- `dts-operator` -> absorbed into **dts-commander** (long-running K8s operator + ops hub)
- `dts-cli install/backup/bundle` -> absorbed into **dts-bootstrap** (one-time installer + emergency repair)
- `dts-cli` remaining commands -> integrated as **dts-commander CLI** (talks to commander API)

The service boundaries rules file must be updated to reflect this change upon approval.

### 1.5 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scope | Complete infrastructure platform (C) | Covers install/ops/CI-CD/Helm/bundle |
| Repo strategy | Independent repo + submodule (C) | Decoupled development, unified workspace |
| Naming | dts-infra | Clear infrastructure identity |
| Binaries | 2: dts-bootstrap + dts-commander | Bootstrap exits after install; commander long-running |
| Management granularity | Component-level (A) | Commander knows every service individually |
| Handoff | Bootstrap deploys commander only (C) | Commander handles all subsequent deployment |
| Bootstrap storage | Embedded bbolt (lightweight, encrypted) | Isolated from all DTS components |
| Global PG | Single PG instance, multi-schema | Config DB for all modules; bootstrap deploys it, commander manages it |
| Helm chart strategy | Centralized in dts-infra (A) | Single source of truth for all deployments |
| AI-native | Infra Agent built into commander | Every operation has AI participation path |
| Keycloak | Global identity platform (Layer 1) | Shared by all modules, not dts-stack internal |
| PostgreSQL | Single global config DB, multi-schema | Separate from future analytics/data lake engines |

## 2. Module Classification and Trust Model

### 2.1 Four-Module Architecture

| Module | Nature | Trust Level | Commander View |
|--------|--------|-------------|----------------|
| **dts-infra** | Infrastructure (self) | — | — |
| **dts-studio** | Business module (Design Center, frontend + backend) | builtin | Managed component |
| **dts-stack** | Business module (Agent Capability Center, 25 services) | builtin | Managed component |
| **app-stack** | Business module (Customer Agents, industry Packs) | builtin or third-party | Managed component |

### 2.2 Component Classification

```go
type ComponentKind string
const (
    KindMiddleware ComponentKind = "middleware"  // Infrastructure services
    KindPlatform   ComponentKind = "platform"    // First-party platform services
    KindAppPack    ComponentKind = "apppack"     // Capability packs (first or third-party)
)

type TrustLevel string
const (
    TrustBuiltin    TrustLevel = "builtin"     // Built-in, no review needed
    TrustVerified   TrustLevel = "verified"     // Verified (signature + scan passed)
    TrustUnverified TrustLevel = "unverified"   // Unverified, needs review
)
```

| Component | Kind | Trust | Namespace |
|-----------|------|-------|-----------|
| Global PG / Kafka / ClickHouse / Neo4j / MinIO | middleware | builtin | dts-system |
| Keycloak | middleware | builtin | dts-system |
| dts-gateway / dts-platform / ... (Java 10) | platform | builtin | dts-system |
| dts-studio-server / dts-studio-webapp | platform | builtin | dts-system |
| dts-agent / dts-intent-engine / ... (Python 10) | platform | builtin | dts-system |
| metro-stack (first-party) | apppack | builtin | dts-packs |
| Third-party Packs | apppack | unverified -> verified | dts-packs |

### 2.3 AppPack Import Flow

Third-party Packs follow a two-phase process — fast import + async review:

**Fast Import (seconds):**
1. `dts pack import <bundle>` — upload to commander
2. Signature verification + manifest format validation (local, &lt;5s)
3. Register as "pending" status — visible to admin

**Async Review (background):**
4. Image security scan (Trivy)
5. Sandbox trial run + health check
6. Complete — status changes to "ready"

**Deployment:**
7. Admin confirms -> deploy to `dts-packs` namespace

Platform components (studio/stack) skip the review pipeline entirely — CI/CD guarantees quality.

## 3. Binary Responsibilities

### 3.1 dts-bootstrap

```
Runtime:       Outside cluster (ops laptop / jump server)
Storage:       Embedded bbolt (local file, encryptable)
Lifecycle:     Run-to-completion, idempotent (re-entrant)

Commands:
  precheck     Environment validation (K8s API, storage class, ports, DNS, resources)
  install      Deploy global-pg -> deploy commander -> wait ready
  status       Check bootstrap execution history
  reset        Clean up failed installation (idempotent rollback)
  repair       Emergency commander redeployment at specific version
  backup       Emergency global-pg backup (when commander is down)

Does NOT:
  × Deploy business middleware
  × Deploy any business services
  × Handle upgrades/rollbacks
```

### 3.2 dts-commander

```
Runtime:       K8s Deployment (dts-system namespace)
Storage:       Independent PG instance (deployed by bootstrap)
Lifecycle:     Long-running, HA (multi-replica leader election)

Functional Areas:

  Middleware Management:
    deploy/remove/upgrade/scale
    Global PG, Kafka, ClickHouse, Neo4j, MinIO, Keycloak

  Platform Component Management:
    deploy/remove/upgrade/scale/rollback
    dts-studio (frontend + backend services)
    dts-stack (25 Java/Python services)
    Fine-grained: individual service level operations

  AppPack Management:
    import -> async scan -> register
    deploy/upgrade/uninstall
    Pack Store catalog
    Third-party trust chain: signature + scan + sandbox

  Operations:
    Health inspection (component -> module -> cluster, 3-level aggregation)
    Upgrade engine (topology sort, rolling/canary, auto-rollback)
    Offline bundling (all 4 modules unified)
    CI/CD pipeline (image build + chart publish)
    Version compatibility matrix
    Audit log (all deployment ops, append-only)

  Infra Agent (AI-native):
    Natural language operations
    Fault diagnosis
    Intelligent upgrade planning
    Pack security review
    Capacity planning
    Incident response
```

### 3.3 Bootstrap-to-Commander Handoff

```
Ops Engineer          bootstrap              K8s              commander
   |                      |                    |                   |
   +- bootstrap install ->|                    |                   |
   |                      +- precheck -------->|                   |
   |                      +- deploy global-pg >|                   |
   |                      +- deploy commander >|---- start ------->|
   |                      +- wait ready <------|<-- health ok -----|
   |  <- "done, use commander"                 |                   |
   |                      |(exits)             |                   |
   |                                           |                   |
   +--- (via commander API/CLI) ------------------------------>|
   |         deploy middleware                 |<-- PG/Kafka/... --|
   |         deploy dts-stack                  |<-- 25 services ---|
   |         deploy dts-studio                 |<-- studio svc ----|
   |         import apppack                    |   async scan...   |
   |         deploy apppack                    |<-- Pack Pod ------|
```

### 3.4 Commander Self-Upgrade

```
Flow:
  1. Commander receives self-upgrade command
  2. Pre-check:
     - New version image pullable
     - New version compatible with current global-pg infra schema
     - No in-flight deploy/upgrade tasks (drain queue)
  3. Execute DB migration (forward-only, N and N+1 compatible)
     - Migrations tracked via version table (schema_migrations)
     - Each migration wrapped in a transaction (atomic apply or full rollback)
     - CI enforces: every migration must pass N/N+1 dual-version compatibility test
     - Partial migration = transaction rollback = safe to retry
  4. Update own Deployment image tag
     -> K8s rolling update: new Pod ready -> old Pod terminated
  5. New commander self-check:
     - PG connection OK
     - All component states readable
     - Health inspection restored
  6. Self-check failure -> K8s auto-rollback to old ReplicaSet

Emergency fallback:
  dts-bootstrap repair --commander-version v1.0.0
  (bootstrap acts as emergency tool, bypasses commander to operate K8s directly)
```

## 4. Dependency Topology

### 4.1 Deployment Layer Order

```
Layer 0 (bootstrap deploys):
  global-pg -> dts-commander
  (bootstrap deploys the single global PG instance, then commander)
  (commander uses schema "infra" within this PG)

Layer 1 (global identity + config, commander deploys):
  keycloak (uses global-pg / keycloak schema)

Layer 2 (analytics middleware, deployed on demand):
  kafka --- clickhouse
  neo4j
  minio

Layer 3 (Java base, sequential):
  dts-platform -> dts-gateway -> dts-ontology-store
                              -> dts-data-security
                              -> dts-audit-log
                              -> dts-query-service
                              -> dts-workflow
                              -> dts-governance
                              -> dts-asset
                              -> dts-data-service

Layer 4 (Python AI, depends on Layer 3):
  dts-intent-engine -> dts-agent
  dts-ontology-engine
  dts-query-ai
  dts-data-connector
  dts-data-quality
  dts-scheduler
  dts-ai-eval
  dts-observability

Layer 5 (Studio):
  dts-studio-server -> dts-studio-webapp

Layer 6 (AppPacks, depends on Layer 3+4):
  metro-stack / prs-stack / third-party Packs
```

### 4.2 Deployment Rules

- Components within the same layer with no mutual dependency deploy in parallel
- Any component failure blocks subsequent layers, does not affect already-deployed components
- Upgrade order: upgrade dependencies first, then dependents (reverse topology check)

### 4.3 Version Compatibility Matrix

```
dts-infra v1.0.0:
  dts-stack: >=1.0.0 <2.0.0
  dts-studio: >=1.0.0 <2.0.0
  middleware:
    postgresql: 16.x
    kafka: 3.7.x (KRaft)
    clickhouse: 24.x
    neo4j-ce: 5.x
    minio: 2024.x (pinned for offline reproducibility)
    keycloak: 24.x || 25.x
  apppack-api: >=1.0.0 <2.0.0
```

Pre-upgrade validation: commander checks target version against compatibility matrix. Incompatible -> reject with explanation.

## 5. Storage Architecture

### 5.1 Global Config PG (single instance, multi-schema)

There is ONE PostgreSQL instance for all configuration data. Bootstrap deploys it at Layer 0,
commander manages it thereafter. All services connect to the same PG with different schemas.

**High Availability**: Global PG is deployed as a StatefulSet with:
- Streaming replication (1 primary + 1 standby minimum for production)
- Automated failover via Patroni (or PG native HA in simpler deployments)
- Automated daily backups to MinIO (once MinIO is available) or local PV
- Commander monitors PG health as a CRITICAL component — PG down = highest severity alert

**Impact of PG unavailability**: All services lose config/metadata access. Java services may
serve cached data briefly; new requests requiring auth/permission checks will fail.
This is an accepted trade-off — operational simplicity over distributed resilience.
Future: if needed, split into 2 PG instances (infra + business) as a non-breaking evolution.

```
Global Config PG (Layer 0, deployed by bootstrap, managed by commander):
  schema: infra          # Commander: deploy records, audit, component state
  schema: keycloak       # Identity and authentication
  schema: platform       # dts-platform: users, roles, menus, tenants
  schema: ontology       # dts-ontology-store: type definitions
  schema: governance     # dts-governance: metadata, lineage
  schema: workflow       # dts-workflow: approval flows
  schema: audit          # dts-audit-log: audit records
  schema: asset          # dts-asset: data asset catalog
  schema: security       # dts-data-security: permission rules
  schema: studio         # dts-studio: design configuration
```

### 5.2 Analytics Layer (separate, future-flexible)

```
Analytics engines (dts-stack selects per need):
  ClickHouse             # Time-series / OLAP (confirmed)
  Data lake TBD          # Iceberg / Hudi / Delta Lake?
  Vector DB TBD          # pgvector -> Milvus?
```

### 5.3 Key Distinction

- **Global Config PG** = "how the system runs" (metadata, permissions, approvals, audit)
- **Analytics engines** = "business data itself" (time-series, graph, vector, lake)

Service-to-storage mapping by data nature:
- `dts-platform`, `dts-gateway`, `dts-ontology-store` -> Global Config PG
- `dts-query-service` -> ClickHouse / data lake (analytics)
- `dts-ontology-engine` -> Neo4j + pgvector (graph + vector)

## 6. Commander Management API

### 6.1 gRPC Services

```protobuf
// infra/v1/cluster.proto
service ClusterService {
  rpc GetStatus(Empty)              returns (ClusterStatus);
  rpc GetTopology(Empty)            returns (TopologyGraph);
  rpc GetCompatibility(Empty)       returns (CompatibilityMatrix);
  rpc UpgradeCommander(UpgradeReq)  returns (UpgradeProgress);
  rpc RepairCheck(Empty)            returns (RepairReport);
}

// infra/v1/component.proto
service ComponentService {
  rpc List(ListFilter)              returns (ComponentList);
  rpc Get(ComponentId)              returns (Component);
  rpc Deploy(DeployReq)             returns (stream Progress);
  rpc Remove(ComponentId)           returns (stream Progress);
  rpc Upgrade(UpgradeReq)           returns (stream Progress);
  rpc Rollback(RollbackReq)         returns (stream Progress);
  rpc Scale(ScaleReq)               returns (Component);
  rpc Restart(ComponentId)          returns (Component);
  rpc GetHealth(ComponentId)        returns (HealthDetail);
  rpc GetLogs(LogReq)               returns (stream LogEntry);
}

// infra/v1/pack.proto
service PackService {
  rpc Import(stream PackChunk)      returns (PackImportResult);
  rpc List(PackFilter)              returns (PackList);
  rpc Get(PackId)                   returns (PackDetail);
  rpc Deploy(PackDeployReq)         returns (stream Progress);
  rpc Upgrade(PackUpgradeReq)       returns (stream Progress);
  rpc Uninstall(PackId)             returns (stream Progress);
  rpc GetScanResult(PackId)         returns (ScanReport);
  rpc Approve(PackId)               returns (PackDetail);
  rpc Reject(RejectReq)             returns (PackDetail);
}

// infra/v1/bundle.proto
service BundleService {
  rpc Create(BundleSpec)            returns (stream Progress);
  rpc Import(stream BundleChunk)    returns (BundleImportResult);
  rpc List(Empty)                   returns (BundleList);
}

// infra/v1/pipeline.proto
service PipelineService {
  rpc Build(BuildReq)               returns (stream Progress);
  rpc Publish(PublishReq)           returns (PublishResult);
  rpc ListBuilds(BuildFilter)       returns (BuildList);
}

// infra/v1/audit.proto
service InfraAuditService {
  rpc Query(AuditQuery)             returns (AuditEntries);
}

// infra/v1/agent.proto
service InfraAgentService {
  rpc Chat(ChatRequest)               returns (stream ChatResponse);
  rpc Diagnose(DiagnoseRequest)       returns (DiagnosisReport);
  rpc PlanUpgrade(UpgradePlanReq)     returns (UpgradePlan);
  rpc ReviewPack(PackReviewReq)       returns (PackReviewReport);
  rpc AnalyzeTrend(TrendRequest)      returns (TrendReport);
  rpc ExplainAlert(AlertId)           returns (AlertExplanation);
}
```

### 6.2 CLI Mapping

```bash
# Cluster
dts cluster status
dts cluster topology
dts cluster upgrade --version v1.1.0

# Components
dts component list
dts component list --kind middleware
dts component deploy dts-gateway
dts component upgrade dts-stack --version v1.1.0
dts component rollback dts-agent --to v1.0.2
dts component health dts-platform
dts component logs dts-gateway -f
dts component scale dts-query-service --replicas 3

# AppPack
dts pack import ./metro-pack-v3.0.1.tar.gz
dts pack list
dts pack scan-result metro-stack
dts pack approve metro-stack
dts pack deploy metro-stack
dts pack uninstall metro-stack

# Offline
dts bundle create --version v1.0.0
dts bundle import ./dts-offline-v1.0.0.tar.gz

# CI/CD
dts pipeline build dts-gateway
dts pipeline publish dts-gateway --tag v1.0.0

# Backup
dts cluster backup
dts cluster backup --module dts-stack
dts cluster restore --from <backup-id>
dts cluster backup list

# Audit
dts audit query --since 24h --kind apppack

# AI Operations
dts agent chat "why is dts-gateway returning 502?"
dts agent diagnose dts-gateway
dts agent plan-upgrade dts-stack --to v1.1.0
dts agent review-pack metro-stack
```

### 6.3 Access Control

| Role | Permissions |
|------|-------------|
| infra-admin | All operations |
| infra-operator | deploy/upgrade/rollback/scale/health/logs/bundle |
| pack-manager | pack import/approve/reject/deploy/uninstall |
| viewer | Read-only (status/list/get/health/logs/audit query) |

Roles managed by Keycloak; commander authenticates via JWT realm_role.

### 6.4 Security Exception: Pre-Gateway Authentication (Iron Law 2)

Iron Law 2 requires all requests pass through dts-gateway. Commander is an exception because:
- Commander operates at Layer 0, **before** dts-gateway exists
- Commander deploys and manages dts-gateway itself — circular dependency if it depended on gateway

**Mitigation**:
- Commander performs its own JWT validation against Keycloak directly
- Commander gRPC endpoints use the same JWT verification logic as dts-gateway
- Once gateway is deployed, external CLI traffic MAY optionally route through gateway
  (configurable, not mandatory — direct access preserved for emergency scenarios)
- This exception is reviewed and documented; no other service may bypass gateway

## 7. Offline Deployment and Bundle Strategy

### 7.1 Bundle Types

```
1. Full Bundle (complete delivery):
   dts-full-v1.0.0.tar.gz
   +- manifest.json
   +- bin/                  (helm, kubectl, optional rke2)
   +- images/
   |   +- infra/            (commander + global-pg + keycloak)
   |   +- middleware/        (kafka, clickhouse, neo4j, minio)
   |   +- stack/            (25 service images)
   |   +- studio/           (studio frontend + backend images)
   |   +- packs/            (approved AppPack images)
   +- charts/               (all Helm charts)
   +- migrations/           (global-pg schema DDL)
   +- scripts/              (push-images.sh, load-images.sh)

2. Module Bundle (single-module upgrade):
   dts-stack-v1.1.0.tar.gz
   dts-studio-v1.1.0.tar.gz
   dts-infra-v1.1.0.tar.gz

3. Pack Bundle (capability pack delivery, for partners):
   metro-pack-v3.0.1.tar.gz
   +- pack-manifest.yaml
   +- images/
   +- chart/
   +- signature
```

### 7.2 Offline Deployment Scenarios

**Scenario 1: Fresh offline install**
```
1. dts-bootstrap install --bundle dts-full-v1.0.0.tar.gz
   -> unpack -> push images to private registry -> deploy commander
2. dts component deploy --all
   -> commander deploys all components by topology order
```

**Scenario 2: Offline upgrade**
```
1. dts bundle import dts-stack-v1.1.0.tar.gz
   -> commander receives -> push images -> register new version
2. dts component upgrade dts-stack --version v1.1.0
   -> compatibility check -> rolling upgrade
```

**Scenario 3: Partner Pack offline import**
```
1. dts pack import metro-pack-v3.0.1.tar.gz
   -> signature verify -> push images -> register as pending
2. Async scan complete -> status becomes ready
3. dts pack approve metro-stack
4. dts pack deploy metro-stack
```

## 8. CI/CD Pipeline

### 8.1 Build Structure

```
deploy/ci/
+- workflows/
|   +- build-infra.yaml          (commander + bootstrap)
|   +- build-stack-java.yaml     (10 Java services, matrix build)
|   +- build-stack-python.yaml   (10 Python services, matrix build)
|   +- build-studio.yaml         (studio frontend + backend)
|   +- build-frontend.yaml       (3 webapps)
|   +- build-bundle.yaml         (offline bundle)
+- templates/
|   +- java-service.yaml
|   +- python-service.yaml
|   +- frontend-app.yaml
+- config/
    +- registry.yaml             (image registry config)
    +- matrix.yaml               (service list for matrix builds)
```

### 8.2 Build Triggers

- Source change push -> webhook -> commander pipeline API
  -> detect changed services (git diff) -> build only affected images
  -> auto tag: git SHA + branch -> push to registry
- Release build: `dts pipeline build --module stack --tag v1.0.0`
  -> full build + semver tag -> changelog -> optional bundle creation

## 9. Health Inspection and Observability

### 9.1 Three-Level Health Model

```
Level 1: Component
  Per-component health probes:
  - HTTP GET /healthz (Java/Python services)
  - gRPC Health Check (gRPC services)
  - TCP connect (middleware ports)
  - Custom checks (PG: SELECT 1, Kafka: topic list)

Level 2: Module
  Aggregate all components in a module:
  - dts-infra:  commander + global-pg + keycloak
  - dts-stack:  25 services (all green / partial degraded / unavailable)
  - dts-studio: studio services aggregated
  - app-packs:  each Pack independent status

Level 3: Cluster
  Global status = f(all module statuses):
  - HEALTHY   -- all modules normal
  - DEGRADED  -- Python layer degraded but Java works (Iron Law 1: still usable)
  - PARTIAL   -- some core services unavailable
  - CRITICAL  -- core foundation unavailable (global-pg / keycloak / gateway)
```

### 9.2 Alert Rules

```
CRITICAL (immediate notification):
  - global-pg unreachable
  - keycloak unreachable
  - dts-gateway unreachable
  - commander self-anomaly

WARNING (ops attention):
  - any Java service restarts > 3 times/hour
  - any middleware disk > 80%
  - API error rate > 1%
  - component upgrade failure

INFO (recorded):
  - Python service degraded (Iron Law 1 allows)
  - AppPack sandbox scan completed
  - Periodic inspection report
```

### 9.3 Commander Self-Observability

```
Endpoints:
  /metrics     -> Prometheus scrape
  /healthz     -> K8s probes
  gRPC reflect -> debug

Metrics:
  infra_components_total{kind,status}
  infra_deploy_duration_seconds
  infra_upgrade_success_total
  infra_health_check_duration_seconds
  infra_pack_import_total{trust}
  infra_bundle_size_bytes
```

## 10. Infra Agent — AI-Native Operations

### 10.1 Architecture

```
internal/
+- agent/
|   +- engine.go          Agent runtime (LLM invocation)
|   +- context.go         Context builder (component state/topology/logs/metrics -> prompt)
|   +- planner.go         AI-generated operation plans (multi-step)
|   +- executor.go        Plan executor (HITL approval integration)
|   +- memory.go          Ops knowledge memory (historical faults/solutions)
|
+- skills/                Infra AI Skills
|   +- diagnose.go        Fault diagnosis (logs+metrics+topology -> root cause)
|   +- upgrade_advisor.go Upgrade advisor (compatibility+risk+execution plan)
|   +- capacity_planner.go Capacity planning (trend prediction+scaling advice)
|   +- incident_responder.go Incident response (alert -> analysis -> repair)
|   +- security_auditor.go Security audit (Pack scan interpretation+risk)
|   +- deploy_reviewer.go Deploy review (change impact+rollback advice)
|   +- log_analyzer.go    Log analysis (clustering+anomaly pattern detection)
|   +- bundle_optimizer.go Bundle optimizer (dependency analysis+trim advice)
|
+- llm/                   LLM Integration Layer
    +- client.go          LLM client (multi-backend)
    +- ollama.go          Ollama local models (offline scenario)
    +- openai.go          OpenAI-compatible API (online scenario)
    +- router.go          Model router (select model by task complexity)
```

### 10.2 AI-Powered Operation Scenarios

**Fault Diagnosis:**
Alert / human question -> collect context (Pod state, logs, metrics, topology, recent changes)
-> LLM root cause analysis -> generate repair plan (multiple options with risk levels)
-> low risk: auto-execute (e.g. restart Pod) / high risk: HITL approval (e.g. version rollback)
-> post-execution verification -> record to ops knowledge base

**Intelligent Upgrade:**
"Upgrade dts-stack to v1.1.0" -> fetch changelog + compatibility matrix
-> LLM change impact analysis -> generate upgrade plan (order, canary strategy, rollback points)
-> present to ops -> confirm -> step-by-step execution with per-step validation
-> anomaly: AI real-time judgment (continue/pause/rollback)

**Auto Inspection:**
Scheduled / event-triggered -> collect global state snapshot
-> LLM compare against historical baseline, identify abnormal trends
-> generate report: urgent -> alert, advisory -> report, trend -> record

**Pack Intelligent Review:**
Partner imports Pack -> parse manifest -> LLM review permission declarations
-> combine Trivy scan results: vulnerability interpretation
-> sandbox log analysis: behavior anomaly detection
-> generate review report + risk rating + recommendation (approve/reject/modify)

**Natural Language Operations:**
Ops can converse directly:
- "What anomalies in the last 24 hours?"
- "Why is dts-agent memory increasing?"
- "Upgrade metro-stack from v3.0.1 to v3.0.2"
- "Is this new Pack safe?"

### 10.3 LLM Integration Strategy

```
Online:
  Prefer remote API (Claude / GPT / DeepSeek)
  Complex reasoning -> large model; simple classification -> small model

Offline (Iron Law: must support):
  Ollama local deployment
  - Light model (e.g. Qwen2.5-7B) -> log classification, simple Q&A
  - Medium model (e.g. Qwen2.5-32B) -> fault diagnosis, plan generation

Model Router:
  simple  (log classify, status summary) -> local small model
  medium  (fault diagnosis, upgrade plan) -> local medium / remote API
  complex (root cause, security audit)   -> remote large / local large
```

### 10.4 Five Iron Laws in Infra Agent

| Law | Application |
|-----|-------------|
| 1. Human Override | All Agent ops executable manually (CLI bypasses AI); high-risk requires HITL |
| 2. Security | Agent API calls go through Keycloak JWT; Agent has no superuser privileges |
| 3. Data Security | LLM context excludes business data; only component state, metrics, sanitized logs, topology |
| 4. Audit Trail | Every reasoning + decision + execution -> audit log (input context, LLM response, action, result) |
| 5. Capability First | All AI capabilities exposed via gRPC Skill interface; API testable before dialog UI |

## 11. Skills Directory

### 11.1 Structure

```
.skills/
+- README.md                            Skill index
+- diagnose/
|   +- root-cause-analysis.yaml         Root cause analysis
|   +- log-pattern-match.yaml           Log pattern matching
|   +- dependency-impact.yaml           Dependency chain impact analysis
+- upgrade/
|   +- upgrade-planner.yaml             Upgrade plan generation
|   +- changelog-analyzer.yaml          Changelog interpretation
|   +- rollback-advisor.yaml            Rollback advice
+- security/
|   +- pack-reviewer.yaml              Pack security review
|   +- vuln-interpreter.yaml           Vulnerability report interpretation
|   +- permission-auditor.yaml         Permission compliance audit
+- capacity/
|   +- trend-forecaster.yaml           Resource trend prediction
|   +- scale-advisor.yaml              Scaling advice
|   +- cost-optimizer.yaml             Resource cost optimization
+- incident/
|   +- alert-responder.yaml            Alert auto-response
|   +- self-healing.yaml               Self-healing strategy
|   +- postmortem-generator.yaml       Incident postmortem report
+- report/
    +- health-report.yaml              Inspection report
    +- change-summary.yaml             Change summary
    +- compliance-report.yaml          Compliance report
```

### 11.2 Skill Definition Format

```yaml
name: infra/diagnose/root-cause-analysis
type: LLM
risk_level: low
description: |
  Analyze component failure by collecting logs, metrics, topology,
  and recent changes, then identify probable root cause.

input_schema:
  component_id: string
  time_range: string
  symptoms: string[]

output_schema:
  root_cause: string
  confidence: float
  evidence: Evidence[]
  suggested_actions: Action[]

context_sources:
  - component_status
  - pod_logs(tail=500)
  - prometheus_metrics(range=1h)
  - recent_deployments(range=24h)
  - dependency_graph

model_tier: medium          # Resolved by model router to specific model per deployment config
                            # tiers: light (classify/summarize), medium (diagnose/plan), heavy (root cause/audit)

timeout: 60s
```

### 11.3 Hot-Update Mechanism

- Git push -> commander detects `.skills/` changes -> reload
- API upload: `dts skill update <yaml>` -> runtime effective
- AppPack-attached: Packs can bring their own infra skills (e.g. industry-specific inspection rules)
- Version management: git-tracked, commander records active skill versions, supports rollback

## 12. Directory Structure (Complete)

```
dts-infra/
+- cmd/
|   +- commander/                  Long-running ops hub
|   |   +- main.go
|   +- bootstrap/                  One-time install guide
|       +- main.go
|
+- api/v1alpha1/                   CRD definitions
|   +- cluster.go                  DtsCluster
|   +- component.go                DtsComponent
|   +- apppack.go                  AppPack
|
+- internal/
|   +- bootstrap/                  === bootstrap-specific ===
|   |   +- precheck/               Environment validation
|   |   +- installer/              Deploy commander PG + commander
|   |   +- store/                  Lightweight local storage (bbolt)
|   |
|   +- controller/                 === commander controllers ===
|   |   +- middleware/             Middleware lifecycle
|   |   +- platform/              Platform components (studio + stack)
|   |   +- apppack/               AppPack lifecycle
|   |
|   +- registry/                   Component registry
|   |   +- catalog.go             Component catalog + metadata
|   |   +- topology.go            Dependency topology + startup order
|   |   +- compatibility.go       Version compatibility matrix
|   |
|   +- health/                     Health inspection (AI-enhanced)
|   |   +- prober.go              HTTP/gRPC/TCP probes
|   |   +- aggregator.go          Status aggregation (component->module->cluster)
|   |   +- alerter.go             Alerting (Prometheus AlertManager)
|   |
|   +- upgrade/                    Upgrade engine (AI-planned)
|   |   +- planner.go             Upgrade plan generation
|   |   +- executor.go            Rolling execution (canary/rolling)
|   |   +- rollback.go            Rollback (Helm revision + DB migration guard)
|   |
|   +- pack/                       AppPack-specific
|   |   +- importer.go            Fast import (signature + manifest)
|   |   +- scanner.go             Async security scan (Trivy)
|   |   +- sandbox.go             Sandbox trial run
|   |   +- store.go               Pack Store catalog
|   |
|   +- agent/                      Infra Agent core
|   |   +- engine.go              Agent runtime (LLM invocation)
|   |   +- context.go             Context builder
|   |   +- planner.go             AI operation plan generation
|   |   +- executor.go            Plan executor (HITL integration)
|   |   +- memory.go              Ops knowledge memory
|   |
|   +- skills/                     Infra AI Skills (Go implementation)
|   |   +- diagnose.go
|   |   +- upgrade_advisor.go
|   |   +- capacity_planner.go
|   |   +- incident_responder.go
|   |   +- security_auditor.go
|   |   +- deploy_reviewer.go
|   |   +- log_analyzer.go
|   |   +- bundle_optimizer.go
|   |
|   +- llm/                        LLM integration layer
|   |   +- client.go              Multi-backend client
|   |   +- ollama.go              Ollama (offline)
|   |   +- openai.go              OpenAI-compatible (online)
|   |   +- router.go              Model router
|   |
|   +- pipeline/                   CI/CD
|   |   +- builder.go             Image build
|   |   +- publisher.go           Image/chart publish
|   |
|   +- bundle/                     Offline bundling
|   |   +- packager.go            4-module unified packaging
|   |   +- loader.go              Offline import
|   |
|   +- store/                      Commander storage layer
|       +- schema.go              PG schema: infra (DDL + migration)
|       +- repository.go          Deploy records / audit / component state CRUD
|
+- .skills/                        Ops AI skill definitions (hot-updatable)
|   +- README.md
|   +- diagnose/
|   +- upgrade/
|   +- security/
|   +- capacity/
|   +- incident/
|   +- report/
|
+- deploy/
|   +- helm/                       All Helm charts
|   |   +- dts-infra/             Commander + dedicated PG
|   |   +- dts-middleware/         Business middleware
|   |   +- dts-studio/            Studio frontend + backend
|   |   +- dts-stack/             Stack Java/Python services
|   |   +- app-packs/             Pack chart template + approved charts
|   +- ci/                         GitHub Actions definitions
|   +- offline/                    Offline build scripts
|
+- builds/                         Dockerfiles
|   +- commander.Dockerfile
|   +- bootstrap.Dockerfile
|
+- proto/                          Commander management API (gRPC)
|   +- infra/v1/
|       +- cluster.proto
|       +- component.proto
|       +- pack.proto
|       +- bundle.proto
|       +- pipeline.proto
|       +- audit.proto
|       +- agent.proto
|
+- go.mod
+- go.sum
+- README.md
```

## 13. Backup and Restore

### 13.1 Commander API

```protobuf
// Add to infra/v1/cluster.proto
service ClusterService {
  // ... existing RPCs ...
  rpc Backup(BackupReq)             returns (stream Progress);     // Full or selective backup
  rpc Restore(RestoreReq)           returns (stream Progress);     // Restore from backup
  rpc ListBackups(BackupFilter)     returns (BackupList);          // List available backups
}
```

### 13.2 CLI

```bash
dts cluster backup                           # Full backup (global-pg + configs)
dts cluster backup --module dts-stack        # Module-specific backup
dts cluster restore --from <backup-id>       # Restore
dts cluster backup list                      # List backups
```

### 13.3 Backup Targets

- **Global PG**: pg_dump per schema -> compressed -> MinIO (or local PV if MinIO unavailable)
- **Helm release state**: helm get values -> stored alongside PG backup
- **Keycloak realm export**: realm JSON export
- **Scheduled**: commander runs daily automated backup; retention configurable (default 7 days)
- **Emergency**: bootstrap can trigger PG backup when commander is down

## 14. Infra Agent Resource Controls

### 14.1 LLM Rate Limiting

```
Per-operation token budget:
  diagnose:        max 8K input + 4K output tokens
  upgrade_plan:    max 12K input + 8K output tokens
  pack_review:     max 8K input + 4K output tokens
  chat:            max 4K input + 2K output per turn
  health_report:   max 6K input + 4K output tokens

Global limits:
  max concurrent LLM calls:  3
  max daily token budget:    500K tokens (configurable)
  cooldown on budget exceeded: degrade to rule-based fallback

Rate limiting:
  max 10 LLM calls per minute per user
  max 100 LLM calls per hour cluster-wide
```

### 14.2 LLM Unavailability Fallback

When LLM is unavailable (offline Ollama down, API unreachable, budget exceeded):
- Agent skills degrade to **rule-based mode** (pattern matching, threshold checks)
- Commander core operations (deploy/upgrade/health) work without AI — AI is enhancement, not dependency
- Status clearly indicates "AI-assisted features degraded" in health report
- Aligns with Iron Law 1: human can always operate manually

### 14.3 Audit of AI Operations

All Infra Agent operations are logged to the same append-only audit trail:
- Input context sent to LLM (sanitized, no business data)
- LLM response (full text)
- Actions taken based on LLM recommendation
- Human approval/rejection decisions
- Token usage per call

## 15. Migration Plan from dts-stack/infra

### 15.1 Current State

Existing Go code at `dts-stack/infra/` (13 files, skeleton implementation):
- `cmd/cli/main.go` — Cobra CLI with stub commands
- `cmd/operator/main.go` — Minimal operator entry point
- `api/v1alpha1/types.go` — CRD type definitions
- `internal/controller/` — Cluster + AppPack controllers (in-memory, no K8s client)
- `internal/health/monitor.go` — Health check skeleton

### 15.2 Migration Steps (Phase 1)

1. Create `github.com/billyhotjava/dts-infra` repository
2. Copy applicable code from `dts-stack/infra/` preserving git history (`git filter-branch` or `git subtree split`)
3. Restructure into new directory layout (cmd/commander, cmd/bootstrap, etc.)
4. Remove `dts-stack/infra/` directory
5. Add dts-infra as submodule in rdc repo
6. Update `buf.gen.yaml` to include Go proto generation for infra protos
7. Update `.rules/10-architecture/service-boundaries.rules` to reflect new naming
8. Update worklog README to reflect new module structure

### 15.3 No State Migration Needed

Current code is skeleton-only with no production state. No data migration required.

## 16. CRD vs PG State Model

Commander uses **PG as source of truth** for all component state, deployment records, and audit logs.
CRDs (DtsCluster, DtsComponent, AppPack) serve as the **K8s-native API interface**:
- Users/tools apply CRD manifests to declare desired state
- Commander watches CRD changes and reconciles actual state
- Reconciliation results written to PG (history, audit)
- CRD `.status` field reflects current state from PG

This follows standard operator pattern: CRDs for declarative API, PG for rich queryable state.

## 17. Implementation Phases (PDCA)

| Phase | Scope | Goal |
|-------|-------|------|
| **Phase 1** | dts-bootstrap | From zero to commander running — environment precheck, deploy commander PG + commander |
| **Phase 2** | dts-commander | Middleware management, platform component deploy/health/upgrade, Infra Agent basic capabilities |
| **Phase 3** | dts-stack prototype | First version of core Java + Python services, integrated with commander |
| **Phase 4** | app-stack prototype | First AppPack (metro-stack), import/deploy flow, partner trust chain |
| **Phase 5+** | PDCA iterations | Continuous improvement: advanced AI skills, offline bundle, CI/CD, capacity planning |

Each phase follows sprint-workflow: Sprint > Feature > Task > IT structure.
After each phase, review (Check) and adjust (Act) before proceeding.

## 18. Infrastructure as Ontology

### 18.1 Core Principle

DTS uses itself to manage itself. Infrastructure is modeled through the same Ontology + Intent + Agent
pattern provided to customers. Infra is "just another industry" alongside energy/manufacturing/research.

```
Customer world:  Transformer ontology → predict-load skill → auto-dispatch
DTS own world:   Node/Pod ontology    → diagnose skill     → self-healing

Same engine: same DAP protocol, same Skill framework, same Ontology store.
```

### 18.2 Infra Ontology ObjectTypes

```yaml
# Namespace: infra/*

# Physical layer
infra/Node:
  properties: [cpu_total, cpu_used, memory_total, memory_used, disk_total, disk_used, os, kernel]

infra/PersistentVolume:
  properties: [capacity, used, storage_class, status, bound_to]

# Container layer
infra/Pod:
  properties: [name, namespace, status, restarts, cpu_request, cpu_limit, memory_request, memory_limit, node]

infra/Deployment:
  properties: [name, namespace, replicas_desired, replicas_ready, image, version, strategy]

# Service layer
infra/DtsService:
  properties: [name, module, kind, version, health, latency_p95, error_rate, uptime]

infra/Middleware:
  properties: [type, version, connections_active, connections_max, storage_used, replication_lag]

# Relationships
Node --hosts--> Pod
Pod --belongs_to--> Deployment
Deployment --implements--> DtsService
DtsService --depends_on--> DtsService
DtsService --uses--> Middleware
Middleware --runs_on--> Node
```

### 18.3 Infra DataBindings

Infrastructure ontology binds to K8s API, Prometheus, and Loki as data sources:

```yaml
sources:
  - type: kubernetes    # K8s Watch API (real-time)
  - type: prometheus    # Metrics (polling 15s)
  - type: loki          # Logs (query on demand)

sync_policies:
  kubernetes: watch (real-time event stream)
  prometheus: polling (15s interval, stored in ClickHouse)
  loki: on-demand (log queries for diagnosis)
```

Quality bindings on infra ontology drive self-healing:
- `Node.cpu_used > 90%` → CRITICAL alert → trigger `infra/diagnose/root-cause-analysis`
- `Pod.restarts > 3/hour` → WARNING → trigger `infra/incident/alert-responder`
- `Middleware.storage_used > 80%` → WARNING → trigger `infra/capacity/scale-advisor`

### 18.4 Self-Healing Loop (Ontology-Driven)

```
1. Perceive:  DataBinding syncs real-time state into infra ontology
2. Detect:    Quality rules on ontology properties trigger alerts
3. Diagnose:  Infra Agent traverses ontology relationship graph for root cause
4. Plan:      Agent generates repair plan with risk-leveled actions
5. Execute:   Actions execute within risk_level constraints (Law 1)
6. Verify:    Ontology state re-checked to confirm resolution
7. Learn:     Pattern recorded to ops knowledge base for future matching
```

### 18.5 Implications for Studio

Studio supports `infra/*` namespace as first-class citizen:
- `object_types` table: namespace column supports `infra/`, `energy/`, `mfg/`, `research/`
- `data_bindings` table: source_type supports `kubernetes`, `prometheus`, `loki`
- `quality_bindings` table: action_type supports `self-healing` with risk_level
- Infra ontology seed data maintained in `.memory/ontology/industry/infra/`
- Infra skills registered through studio, same lifecycle as business skills

## 19. Five Iron Laws Compliance Summary

All dts-infra design decisions are governed by `.rules/00-foundation/five-iron-laws.rules`.
Infrastructure-specific application is codified in `.rules/10-architecture/infra-iron-laws.rules`.

| Law | Key Constraint on dts-infra |
|-----|----------------------------|
| 1. Human Override | AI is enhancement layer; commander core works without LLM; self-healing bounded by risk_level; ai-mode kill switch |
| 2. Security | Commander validates JWT directly (pre-gateway exception); Agent uses scoped RBAC; AppPacks cannot access commander API |
| 3. Data Security | Infra data classified (PUBLIC→TOP-SECRET); LLM context sanitized; ontology queries filtered by role |
| 4. Audit Trail | All ops append-only in commander PG; Agent reasoning fully logged; bootstrap audit synced on recovery; 3-year retention |
| 5. Capability First | gRPC API + CLI before any UI; skills testable before automation; no demo-only features |

### AI Mode Switch

```bash
dts cluster set ai-mode=disabled     # AI off, manual only
dts cluster set ai-mode=advisory     # AI suggests, human decides (default)
dts cluster set ai-mode=autonomous   # AI executes within risk_level bounds
```

## Appendix A: Keycloak as Global Identity Platform

Keycloak is promoted from dts-stack internal component to global identity platform:

```
dts realm (single realm, unified identity)
  |
  +- Clients
  |   +- dts-commander          infra management API
  |   +- dts-studio             design center
  |   +- dts-gateway            stack API gateway
  |   +- dts-admin-webapp       admin frontend
  |   +- dts-cortex-webapp      analytics frontend
  |   +- dts-pilot-webapp       decision frontend
  |
  +- Realm Roles
  |   +- infra-admin / infra-operator / pack-manager   infrastructure roles
  |   +- platform-admin / data-admin / security-admin   platform roles
  |   +- analyst / decision-maker                       business roles
  |   +- pack-developer                                 partner role
  |
  +- Identity Providers (optional)
      +- LDAP/AD       enterprise directory
      +- SAML          federated auth
      +- PKI/cert      offline scenario
```

## Appendix B: Global Config PG Schema Isolation

All modules share one PG instance but use separate schemas:

```sql
-- Commander creates schemas during component deployment
CREATE SCHEMA IF NOT EXISTS infra;
CREATE SCHEMA IF NOT EXISTS keycloak;
CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS ontology;
CREATE SCHEMA IF NOT EXISTS governance;
CREATE SCHEMA IF NOT EXISTS workflow;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS asset;
CREATE SCHEMA IF NOT EXISTS security;
CREATE SCHEMA IF NOT EXISTS studio;

-- Each service connects with its own credentials, restricted to its schema
-- GRANT USAGE ON SCHEMA <name> TO <service_user>;
```

This provides logical isolation while sharing a single PG instance for operational simplicity.
Analytics workloads use separate engines (ClickHouse, future data lake) — never the config PG.

:::
