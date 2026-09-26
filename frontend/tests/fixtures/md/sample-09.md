# DTS Wiki — project instructions

Internal Confluence-style wiki for DTS (future DTS knowledge center). JHipster 9 monolith
backend (skipClient) + React/antd frontend in `frontend/` bundled into the same jar,
PostgreSQL as source of truth, bidirectional sync with product git
repositories (`docs/`, `worklog/`). Planning lives in the dts-rdc repo:
`worklog/v1.0.0/sprint-6-202610/`.

## Read first (design lives in the dts-rdc worklog; this repo is its submodule `dts-wiki/`)
1. `../worklog/v1.0.0/sprint-6-202610/features/F0-基线与技术选型spike/design/08-编码任务与交接说明.md` — work order, hard constraints, definition of done
2. `../worklog/v1.0.0/sprint-6-202610/features/F0-基线与技术选型spike/design/00`–`07` — architecture decisions, domain model, backend, git sync, frontend, deployment, tests
3. `jhipster/dts-wiki.jdl` — the only place to change entities (regenerate after editing)

If this repo is checked out on its own, the same files are at
https://github.com/billyhotjava/dts-rdc/tree/main/worklog/v1.0.0/sprint-6-202610/features

## Hard rules
- Documents: everything development-related (design, plans, spike results, task notes, acceptance evidence)
  goes into the dts-rdc `worklog/` (Sprint-6 feature/task, `assets/`, `it/`). This repo's `docs/` holds only
  formal documents: product/user documentation, external docs, operations runbook.
- Entities change only via the JDL + generator; hand edits to generated code are marked `// DTS-WIKI: customized`.
- Business endpoints only under `/api/wiki/**` and always through `SpaceAccessService`; generated entity endpoints are ROLE_ADMIN only.
- Git commands only in `service.wiki.sync`; never `push --force`; writes only inside configured sync roots.
- Page versions are immutable; identical content (sha256) never creates a version.
- Server 10.20.0.50: no `docker pull` (broken daemon proxy — ship images with `docker save | ssh docker load`), never restart dockerd, never touch the running wiki (`/data/dts-wiki`, port 18090), Jira or Keycloak containers. This project uses `/data/dts-wiki-v2` and port 18091.
- No secrets in git (`.env`, deploy keys, client secrets).
- Versions: stable releases only (DTS rule R-012); check licenses of new dependencies.

## Conventions
- Talk to the product owner in Chinese; code comments and docs in English; UI strings via i18n (zh-cn default).
- Branches `feat/W<n>-<topic>`; commits `feat(F<n>/T<nn>): <description>` referencing Sprint-6 tasks.
- After each work package: update the task status and put real evidence (outputs, screenshots, SHAs) into dts-rdc `worklog/v1.0.0/sprint-6-202610/it/`.
