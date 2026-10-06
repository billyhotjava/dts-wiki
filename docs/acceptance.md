# Runtime acceptance

Run the operator tools from the reviewed Wiki checkout. Python 3 uses only the
standard library. Tokens stay in external environment configuration; never put
token values in a plan, shell command argument, report or repository file. The
tools never mint credentials, change identity membership or follow redirects.
TLS verification stays enabled. Use the approved instance origin and personal
identities provisioned through the existing OIDC/PKCE flow.

## Read-only smoke

Create a credential-free plan outside the source checkout. Replace the neutral
example slugs and page IDs with actual expectations from the content manifest
and approved membership matrix. Include an allowed and denied personal account
for every declared space; an administrator alone cannot prove isolation.

```json
{
  "identities": [
    {
      "label": "team-reader",
      "tokenEnv": "WIKI_ACCEPTANCE_READER_TOKEN",
      "allow": ["team"],
      "deny": ["other-team"],
      "pages": [{"id": 123, "space": "team", "gitReadOnly": true}],
      "mcp": true
    }
  ]
}
```

The referenced environment variable contains the externally supplied personal
access token. Use separate entries/tokens for the different permission sets.
`mcp: true` requires a personal agent token accepted by both the regular API and
MCP audiences; `false` explicitly excludes MCP from that identity's smoke scope.

```sh
tools/acceptance-smoke --base-url https://your-wiki.example \
  --expected-commit <application-commit> --plan /secure/wiki-acceptance.json \
  --search '<approved-term>'
```

The command checks health/readiness, anonymous API rejection, the deployed Git
commit, allowed/denied spaces, page ownership/read-only presentation, immutable
history list shape, search scope and requested personal MCP reads. MCP POSTs
only initialize, discover tools and read pages. It never publishes content or
creates OAuth clients. Failed checks remain FAIL; missing plan/token configuration
returns GAP. JSON output excludes tokens, document bodies and personal profiles.
A PASS applies to the submitted plan; it is not full browser/SMTP/cutover DoD.

## Latency and controlled saves

Set `WIKI_BENCHMARK_TOKEN` in external configuration. Default execution performs
only reads, requires at least 10,000 authorized pages and measures 50 concurrent
workers with 10 requests each. Do not lower the scale gate for release evidence.

```sh
tools/acceptance-benchmark --base-url https://your-wiki.example \
  --page-id <representative-page> --search '<approved-term>'
```

To measure save latency, explicitly authorize creation in a disposable acceptance
space with `--save-space <approved-disposable-space>`. Each worker creates a new
native root page with a unique run title, uses exact base versions for subsequent
updates and soft-deletes only its own acknowledged, ownership-validated page.
Existing pages, Git content and database schema are never modified by this mode.
Create/cleanup time is excluded from the save samples. CSRF cookies are retained
and the corresponding header is supplied where required.

```sh
tools/acceptance-benchmark --base-url https://your-wiki.example \
  --page-id <representative-page> --search '<approved-term>' \
  --save-space <approved-disposable-space>
```

Budgets are page P95 <200 ms, search P95 <800 ms and save API P95 <1,000 ms.
Measurements are client HTTP round trips, not isolated database commit timings.
The save budget is a conservative end-to-end companion to the database budget.
Use representative acceptance content separately for large Markdown scenarios;
the default save body is intentionally small and is not a capacity claim.

Cleanup or measurement failures make the overall result FAIL. The report contains
the run ID, retained acknowledged IDs and an unacknowledged-creation flag. A timeout
can leave an unacknowledged probe: inspect pages by that run title before manual
cleanup. Never infer ownership from an arbitrary response ID. Soft deletion keeps
immutable history and activity records; it does not purge acceptance data.

## CI and remaining runtime work

`.github/workflows/verify.yml` invokes the same `build.sh verify` gate on the
trusted `dts-x86` self-hosted runner. The runner must have Java 25, Node 24,
pnpm 12, Docker access, preloaded PostgreSQL/pg_bigm and Ryuk images, and the
pinned Common Pack 1.1.0 artifact in its Maven repository. Register and configure
the runner through the owning Infra S2 workstream. Only main/feature/fix pushes
in the owning repository and explicit dispatches trigger the workflow; fork PR
events do not run on this host. Trusted branch checks supply PR validation.
CI configuration alone is not a successful CI run.

Before closing release acceptance, record real browser OIDC/PKCE login, native
editing/conflicts and Git write rejection, diagram rendering, SMTP if enabled,
one-minute inbound visibility, representative scale/query plans/RSS, a real
backup and isolated restore, previous-binary rollback compatibility and the
approved proxy cutover. These require actual identities, data and the target
environment. No local fixture result substitutes for them.
