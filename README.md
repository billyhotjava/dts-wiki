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
./mvnw clean verify
cd frontend
pnpm test
pnpm build
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

## Release

`deploy/release.sh` builds and transfers a pinned application image. Set
`JAVA_HOME` explicitly if the JDK is installed elsewhere. Runtime configuration
is external to source control; see `deploy/compose.yml` for required variables.
Source verification and runtime deployment are separate steps. The v2 instance
uses port 18091 and must not replace the existing Wiki instance without a
separately approved migration window.
