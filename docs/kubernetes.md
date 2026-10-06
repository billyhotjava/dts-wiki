# Kubernetes runtime

The deployable Helm chart belongs to `dts-infra/charts/dts-wiki`; that module
owns routing, PVCs, connection contracts and offline image distribution. Wiki
owns the `prod,kubernetes` Spring profiles and the migration entry point.

## Runtime contract

- HTTP listens on 8080. The chart exposes only this port through the shared
  Gateway API HTTPRoute. Set the canonical HTTPS origin before installation;
  register its OIDC callback `/login/oauth2/code/oidc` with the identity provider.
- Internal health, build information and Prometheus metrics listen on 9091 under
  `/management`. Only GET probes and metrics are public on this listener. Other
  actuators remain denied. The application listener keeps its existing security
  policy; forwarded client headers cannot select the internal security chain.
- Liveness measures the application process. Readiness also checks the database.
  A PostgreSQL outage removes traffic without causing a liveness restart loop.
- The image runs as UID/GID 1001 with a read-only root filesystem. `/tmp` is an
  emptyDir; `/data` is the attachment and Git-working-copy PVC. Browser session
  cookies are Secure and SameSite=Lax; trusted TLS termination supplies forwarded
  headers. Do not expose the Pod directly to untrusted clients.
- Browser sessions and editing presence are in memory. One replica and Recreate
  rollout are mandatory. Restart requires browser login again; private drafts,
  pages and versions remain in PostgreSQL. Attachments remain on the PVC. This
  release does not promise zero-downtime upgrades or multi-replica operation.

## Ordered migrations

`java -jar /app/app.jar --migrate --spring.liquibase.enabled=true` applies the
existing changelog and exits. It boots no HTTP listener, identity client, JPA
repository, content worker or notification scheduler. Only datasource settings
are required. A failed migration makes the Job fail and blocks installation or
upgrade. Disabling Liquibase in migration mode fails closed.

The application profile disables automatic Liquibase execution. Run the Helm
pre-install/pre-upgrade Job before serving the new binary. An unchanged changelog
is idempotent. Preserve applied changesets and back up the database before any
upgrade. Rolling back the image does not reverse a schema migration; verify the
previous binary against the migrated schema before using Helm rollback.

The external PostgreSQL provider must offer **pg_bigm**, including
`gin_bigm_ops`. Use a dedicated Wiki database/role. Wiki never falls back to any
other module's database. The default platform CNPG image is not sufficient until
its extension compatibility has been verified.

## Content and persistence

Native-only Wiki requires no Git key. Optional inbound import uses an explicit
repository, branch and manifest path plus an external Secret with `content.key`
and pinned `known_hosts`. The chart stages group-readable projected files into
an owner-only temporary directory before the application starts. Git uses
BatchMode and strict host checking; it never appends hosts to a read-only Secret.
Secret rotation requires a rollout. No content or credentials ship in this repo.

The Helm-created data PVC is retained on uninstall. Reinstallation must bind the
retained claim deliberately; uninstall is not a backup. Restore database and
attachment data together into an isolated instance and verify attachment hashes,
page history and authorization. S3 storage is a separate, unfinished migration;
the current application uses local persistent files.

## Verification boundary

Chart rendering, source tests and local kind fixtures establish deployment
mechanics. Site acceptance additionally requires the actual Gateway/TLS/IdP,
network-policy enforcement, PostgreSQL extension image, storage provisioner,
offline registry, backup restore and prior-binary compatibility. Multi-architecture
image publication must precede ARM deployment. No local fixture result establishes
production cutover or company identity permissions.
