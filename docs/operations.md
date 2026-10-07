# Wiki operations

## Prepare and deploy

Use Java 25, Node 24 and pnpm 12 on the build host. Preload
`dts-wiki-db:18-bigm`, `testcontainers/ryuk:0.14.0` and the application JRE base
before building. `build.sh verify` runs frontend tests, the bundle budget,
backend unit/integration tests and checks every bundled frontend file against
the production jar. Tests use disposable databases.

```sh
deploy/release.sh --prepare <unique-tag> /secure/artifacts/wiki-<unique-tag>
```

The new directory contains the saved images, image IDs, source commit, Compose
files and SHA-256 checksums. Preparation does not connect to the deployment
host. `WIKI_APP_BASE_IMAGE` can select a preloaded image ID. Application builds
install Git and SSH tools locally; the target never pulls images.

Before deployment, provision `/data/dts-wiki-v2/.env` using external configuration.
For private SSH sources, also provision the SSH known-hosts file and
`secrets/content.key`. Create `repos/`,
`attachments/` and `secrets/` for container UID 1001; key mode is 0600 and secret
directory mode is 0700. Use the content repository's read-only deploy key.
Supply the manifest URL/path, OIDC issuer/client, current role grants and public
URL. Set `WIKI_OIDC_AUDIENCE` to the audience emitted for the Wiki client
(default `dts-wiki`). Compose lets Liquibase reuse the primary datasource and
its credentials; do not set a separate Liquibase URL without its own credentials.
Configure MCP separately as described in [mcp.md](mcp.md).

Host login and content import use separate keys. Host SSH access is diagnosed
through the owning [Infra runbook](https://github.com/billyhotjava/dts-infra/blob/main/deploy/ssh/README.md).
Register the dedicated content public key on the manifest repository's Settings
→ Deploy keys with **Allow write access disabled**, following
[GitHub's deploy-key guide](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys).
Keep its private key external and readable by container UID 1001 only. Verify
the repository HEAD using `git ls-remote` with that explicit content identity
and strict host-key checking before installing it into v2. Isolate that probe
with `ssh -F /dev/null -o IdentitiesOnly=yes -o IdentityAgent=none -i <content-key>`;
otherwise additional configured identities can make the result misleading. A successful read
does not prove write access is disabled; record the actual deploy-key setting
separately. Never use the developer's general GitHub identity for runtime import.

After the handoff's access, identity and database approval gates have passed:

```sh
deploy/release.sh --deploy /secure/artifacts/wiki-<unique-tag> root@10.20.0.50
```

This loads the checksum-verified images and starts only `dts-wiki-v2`, on port
18091. The persistent `compose.override.yml` selects the exact release tags.
Existing tags cannot be overwritten on the target. The live instance at
`/data/dts-wiki` on 18090, Jira, Keycloak and dockerd are outside this operation.

### Public HTTPS sources and an existing database

Public content repositories support anonymous HTTPS import. Keep the canonical
HTTPS repository URL and TLS verification enabled; no content SSH identity is
needed. The current `release.sh --deploy` preflight requires an SSH content key,
so this path uses an inspected application-only rollout instead of that helper.

Verify the immutable bundle checksums and load its application image with
`docker load`. Preserve the live database container, volume and exact image ID
in the external Compose override, along with the new unique application tag.
Apply only the application with `docker compose up -d --no-deps --pull never
wiki-app` after `docker compose config --quiet`. Run the backup/restore and
migration rehearsal first. Do not load or select a lower database minor version
merely because it is present in a prepared development bundle.

If a verified Git HTTPS peer is reachable while the resolver-selected peer is
not, an external app environment override can set `GIT_CONFIG_COUNT=1`,
`GIT_CONFIG_KEY_0=http.curloptResolve` and
`GIT_CONFIG_VALUE_0=<git-host>:443:<verified-peer-ip>`. This preserves the host
name and certificate verification. Record it as a temporary deployment setting,
retest normal DNS reachability and remove it when the route is repaired.

Verify a full fetch containing new objects, not only `ls-remote` or an unchanged
fetch. The .50 development fallback uses the restricted
[Infra Git relay](https://github.com/billyhotjava/dts-infra/blob/main/deploy/wiki-git-proxy/README.md)
on .6, accepting only .50. Its final Git override sets
`GIT_CONFIG_KEY_0=http.https://github.com/.proxy` and
`GIT_CONFIG_VALUE_0=http://10.20.0.6:10819` with `GIT_CONFIG_COUNT=1`, replacing
the peer pin. This preserves GitHub TLS verification and does not route Java
OIDC/DB requests through the proxy. Monitor the development proxy dependency;
customer Kubernetes must qualify its own approved outbound route or proxy.

The manifest collision guard prevents silently claiming existing native spaces.
For an explicitly identified legacy space with no Git provenance or sync roots,
back up its metadata and data first, then adopt only that verified space by
assigning the configured repository and branch. The next manifest reconciliation
sets its configured role and roots. Native pages outside imported roots keep
their IDs, versions and write access. Do not clear existing pages or relax the
general collision guard.

## Health and acceptance

Check `/management/health`, readiness, the login callback, personal display
name and each manifest space with both allowed and denied accounts. Check
inbound history and a second unchanged cycle, read-only Git writes, native
page save/conflict, search, version restore and the authorized MCP tools.
Local fixture screenshots do not establish these runtime results.

Record container image IDs, Java process RSS, heap, startup duration, concurrent
save/search latency and Git poll duration in the active Sprint evidence.
Initial capacity is one application instance, a 768 MB container limit and a
30-second sync poll. These are configuration limits, not measured capacity.

Alert when readiness fails for 60 seconds, when memory exceeds 85% of the
container limit for 5 minutes, or when an inbound root remains unsuccessful
for more than 3 poll intervals. A manifest validation failure stops the entire
cycle; inspect the inventory and declared roots at its captured commit before
retrying. A missing deploy key, host key or remote permission requires external
configuration repair. Search requires the pinned database's `pg_bigm` extension.
Do not change a migration checksum to work around a failed startup.

Logs should correlate HTTP trace IDs, space/root IDs, captured Git SHA, poll
duration and exception class. Do not log credentials, access tokens, document
bodies or personal OAuth codes. MCP validation failures return protocol errors;
unexpected tool errors expose no internal exception text.

## Backup and restore rehearsal

Run from the v2 instance directory using the helper in its installed release:

```sh
cd /data/dts-wiki-v2
./releases/<tag>/backup.sh /secure/backups/wiki-<timestamp>
./releases/<tag>/restore-check.sh /secure/backups/wiki-<timestamp>
```

Backup briefly stops the Wiki application, dumps its database and archives the
content-addressed attachment store, then resumes the application. It records
page/version/attachment/migration counts, the exact database image ID, Compose
configuration and checksums. The destination must be new and private. Keep it
on encrypted backup storage; database backups contain company content.

The checker validates the archive, restores into a disposable container with
no network and no published ports, checks `pg_bigm`, compares counts and verifies
every blob digest and attachment reference. It removes only its own container
and temporary directory. External OIDC credentials and deploy keys remain in
the external configuration escrow; the helper does not copy `.env` or keys.
Git working copies can be rebuilt from the pinned manifest repository.
The checker requires Python 3 and Docker. If Python is absent on the runtime
host, run it from the trusted build host against copied backup artifacts and an
explicit remote Docker command wrapper. Keep the disposable database isolated
and use the recorded preloaded image; do not install extra runtime software just
to run the rehearsal.

For an actual recovery, obtain the incident owner's recovery window, stop only
the v2 app, restore into a **new** database volume, restore blobs into a **new**
attachment directory, verify permissions/checksums and switch the v2 mounts.
Retain the previous volumes until acceptance. Run login, permissions, versions,
attachments and unchanged inbound-cycle acceptance before reopening writes.
The rehearsal helper intentionally has no command that overwrites a live DB.

## Legacy links and cutover

Set `APPLICATION_WIKI_LEGACY_ALIASES_JSON` to an external JSON object. Each old
space name maps to `{ "spaceSlug": "<current-space>", "rootPath": "<content-root>" }`.
No aliases are enabled by default. `/p/<old-space>/<path>` resolves through the
authenticated API, checks the current destination space permission, and replaces
the browser URL with `/s/<current-space>/p/<id>`. Unknown, deleted and denied
destinations return 404. Paths cannot escape the configured root.

Keep both instances available during acceptance. DNS/proxy cutover requires the
handoff's explicit window. Capture the current proxy configuration before
changing its upstream. Roll back the proxy to the old instance if login, space
isolation, links or health checks fail. For a v2 application rollback, reinstall
the previous release's Compose files and run `docker compose up -d --pull never`.
Additive migrations retain previous columns; rehearse the previous binary with
the migrated database before approving rollback compatibility. Never downgrade
or edit an applied Liquibase changeset in place.
