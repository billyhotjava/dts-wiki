# DTS Wiki

A company-owned lightweight knowledge workspace. Java packages belong to
`com.yuzhi.dts.wiki`; PostgreSQL stores pages and immutable Markdown versions.
The React application lives in `frontend/` and the Spring Boot application in
`src/main/java/`.

## Development

Use Java 25, Node 24 and pnpm 12. Configure PostgreSQL and an external OIDC
provider through environment variables or an untracked configuration file.
Do not commit credentials.

```sh
export JAVA_HOME="$HOME/.sdkman/candidates/java/25.0.4-tem"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

The frontend development proxy targets the backend on port 8080. Authentication
uses OIDC sessions in the browser and audience-validated bearer tokens for API
clients. Space access is enforced by the business API under `/api/wiki`.

## Verification

Backend integration tests use a disposable PostgreSQL container with pg_bigm.
Preload the image before running tests; never point tests at a business database.

```sh
docker build --pull=false -f src/main/docker/postgres.Dockerfile \
  -t dts-wiki-db:18-bigm .
./build.sh verify
```

pnpm's workspace policy permits the pinned esbuild installation script. Other
dependency build scripts require an explicit review.

## Ownership and migrations

Entities, repositories, services and migrations are maintained directly. Add
new Liquibase changesets for schema evolution. Existing applied changesets and
legacy identity table names remain intact so existing databases can upgrade
without changing their migration checksums.

Page writes go through `service/wiki`; generic generated entity CRUD endpoints
have been removed. Content analysis updates metadata and search projections in
the same transaction as a new page version. Saving identical content creates no
additional version.

Wiki stores no product documents or worklog content in its repository. Runtime
space inventories come from the content repository's versioned manifest.
The company-owned implementation replaces the earlier generator-only entity
workflow. Current design and verification evidence are maintained in the active
Sprint 5 worklog in the coordination repository.

## Content synchronization

Set `application.wiki.content.repo-url`, `branch`, `manifest-path` and
`deploy-key-path`, or their `APPLICATION_WIKI_CONTENT_*` environment equivalents.
The manifest and frontmatter schemas are loaded from the pinned
`com.yuzhi.dts:dts-common-pack:1.1.0` release artifact. The content repository
declares space names, access roles and allowed roots; Wiki has no fixed inventory.

Each cycle validates the entire manifest and every root at a captured Git commit
before changing space configuration or importing content. Invalid inventories
stop the cycle. Removed spaces retain their pages and versions but stop syncing.
Files outside declared roots, symlinks, submodule links, hidden files and checksum
files are excluded. Images and supported attachments use the scoped blob store.

Git pages display a read-only label. Their content, tree operations and attachment
writes return `409 GIT_PAGE_READ_ONLY`; native pages remain editable. Outbound
synchronization is disabled by default and is always disabled for manifest-managed
spaces. Install one read-only repository deploy key, never a personal access token.

## Kubernetes

The owning deployment module supplies `dts-infra/charts/dts-wiki`. Activate
`prod,kubernetes` only with the chart's migration Job and external PostgreSQL/OIDC
connection Secrets. PostgreSQL must provide pg_bigm; a standard image without
that extension cannot apply the Wiki changelog. The initial chart uses one
application replica with a retained PVC for attachments and Git working copies.
See [Kubernetes runtime](docs/kubernetes.md) for management-port separation,
migrations, storage, limits and rollout requirements.

Long documents show a collapsible page outline. Heading links remain stable
across document visits, including repeated and Unicode titles.

## Release

`deploy/release.sh --prepare <tag> <new-bundle-dir>` verifies and prepares local
images; `--deploy <bundle-dir> <ssh-target>` is a separate runtime operation.
Set `JAVA_HOME` explicitly if the JDK is installed elsewhere. Runtime configuration
is external to source control; see `deploy/compose.yml` and
[the operations runbook](docs/operations.md) for configuration, backup and rollback.
See [collaboration](docs/collaboration.md) for personal navigation, comments,
current identity checks and optional mail. [Content tools](docs/content-tools.md)
cover Markdown formatting and safe diagram bundles; [MCP](docs/mcp.md) describes
personal agent access.
See [editing continuity](docs/editing.md) for private drafts and soft presence.
See [runtime acceptance](docs/acceptance.md) for identity smoke, deployment-version
checks, read-only latency probes and explicitly scoped disposable save probes.
Source verification and runtime deployment are separate steps. The v2 instance
uses port 18091 and must not replace the existing Wiki instance without a
separately approved migration window.
