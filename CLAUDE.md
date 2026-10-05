# DTS Wiki — project instructions

Product-neutral, Confluence-style wiki shipped as a DTS module. JHipster 9 monolith
backend (skipClient) + React/antd frontend in `frontend/` bundled into the same jar.
PostgreSQL is the source of truth; git is a sync endpoint (inbound only in this phase).
This repository holds **program code only — no content and no hard-coded space names**.
Spaces come from the content source's manifest (wiki-content v1).

## Read first

This repo is the `dts-wiki/` submodule of dts-rdc (`/opt/prod/dts/dts-rdc`). Start with
the work guide; it defines scope, work packages, rules and where to record status:

1. `../dts-worklog/spaces/rdc/worklog/v1.0.0/sprint-5-202610/assets/handoff-20261005-dts-wiki-dev-session.md`
   (GitHub: https://github.com/billyhotjava/dts-rdc/tree/main/dts-worklog/spaces/rdc/worklog/v1.0.0/sprint-5-202610/assets)
2. Design D18–D23 (wiki product vs. content):
   `../dts-worklog/spaces/rdc/worklog/v1.0.0/sprint-5-202610/features/F0-基线仓库落位与架构定案/design/2026-10-05-wiki产品与worklog内容分离及模块关系设计.md`
3. Archived wiki designs 00–10 and the S4a plan (read-only, amended by 1 and 2):
   `../dts-worklog/spaces/rdc/archive/dts-rdc-worklog/v1.0.0/sprint-5-202610/features/F2-Wiki平台骨架身份与性能/design/`,
   `.../F4-Wiki-Git双向同步/design/01-S4a研发文档统一与入站同步实施计划.md`
4. Content contract: `../dts-common/src/main/resources/protocol/wiki-content/` (Common 1.1.0)
5. `jhipster/dts-wiki.jdl` — the only place to change entities (regenerate after editing)

## Hard rules
- No content in this repository (`content/`, `worklog/` are rejected by dts-rdc
  `scripts/check-boundaries.py`); never hard-code space slugs. Development documents,
  task status and evidence go to dts-rdc `dts-worklog/spaces/rdc/worklog/` (Sprint-5
  cards under `features/F2-*`..`F5-*`, evidence under `it/wiki/`). Never edit
  `dts-worklog/spaces/*/archive/` (sealed). This repo's `docs/` holds only formal
  product/operations documents.
- Content source: one repository + manifest (`application.wiki.content.*`); roots are
  relative to the repository root; files outside declared roots are never imported.
- Entities change only via the JDL + generator; hand edits to generated code are marked `// DTS-WIKI: customized`.
- Business endpoints only under `/api/wiki/**` and always through `SpaceAccessService`; generated entity endpoints are ROLE_ADMIN only.
- Git commands only in `service.wiki.sync`; never `push --force`; writes only inside configured sync roots.
- Page versions are immutable; identical content (sha256) never creates a version.
- Server 10.20.0.50: no `docker pull` (ship images with `docker save | ssh docker load`), never restart dockerd, never touch the running wiki (`/data/dts-wiki`, port 18090), Jira or Keycloak containers. This project uses `/data/dts-wiki-v2` and port 18091.
- No secrets in git (`.env`, deploy keys, client secrets).
- Versions: stable releases only (DTS rule R-012); check licenses of new dependencies.
- Do not change the wiki-content contract semantics here; contract changes go through dts-common.

## Conventions
- Talk to the product owner in Chinese; code comments and docs in English; UI strings via i18n (zh-cn default).
- Branches `feat/<wp>-<topic>`; commits `feat(F<n>/T<nn>): <description>` referencing Sprint-5 F2–F5 tasks.
- After each work package: update the task card status and add real evidence (outputs, screenshots, SHAs)
  in dts-rdc `dts-worklog/spaces/rdc/worklog/v1.0.0/sprint-5-202610/it/wiki/`; run
  `dts-common/tools/content-lint check .` and `python3 scripts/check-boundaries.py` in dts-rdc.
