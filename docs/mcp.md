# Personal MCP access

The same application serves a stateless Streamable HTTP endpoint at `/mcp`.
It returns JSON for requests and empty 202 responses for notifications; GET and
DELETE return 405 because there is no SSE stream or server session. Supported
protocol versions: `2025-11-25`, `2025-06-18`, `2025-03-26`.

## Configuration

Enable only after registering the externally managed authorization client:

```yaml
application:
  wiki:
    mcp:
      enabled: true
      resource-uri: https://your-wiki.example/mcp
      client-ids: [dts-wiki-agent]
      allowed-origins: []
```

The canonical resource URI must use HTTPS; localhost HTTP is allowed for local
development. Origin checks use its exact origin and explicit additional origins.
Do not configure wildcards. The regular OIDC issuer remains the token issuer.
Include the canonical resource URI in the application's allowed audience list
when the issuer does not include another already-allowed API audience.

Resource metadata is exposed at `/.well-known/oauth-protected-resource/mcp` and
the root well-known path. A 401 Bearer challenge advertises this metadata URL.
Clients use authorization code with PKCE S256 against the external Keycloak
issuer, the preregistered `dts-wiki-agent` client and exact callback URLs. They
request `openid wiki.read`, adding `wiki.write` only for native edits. Pass the
canonical URI as the OAuth `resource` parameter. Configure Keycloak's audience
mapper for the same URI; verify the resulting access token before enabling MCP.

Tokens must contain the correct issuer, resource audience, authorized client,
expiry, subject and personal `preferred_username`. Service-account identities
are rejected. Browser sessions cannot authorize this endpoint. The application
does not store personal tokens or implement password grants/token minting.

## Tools

Read tools: `wiki_search`, `wiki_get_page`, `wiki_list_tree`, `wiki_query`,
`wiki_get_diagram_spec`. Write tools: `wiki_update_page`, `wiki_create_page`,
`wiki_put_diagram`. Discovery hides write tools without `wiki.write`; every
operation still checks current space authorization and editor permissions.
Git page/attachment writes return a tool error `GIT_PAGE_READ_ONLY`.

Native updates require `baseVersionNo`; version conflicts return the current
version number so callers can reread and merge. Versions use the personal user
as author and the optional `X-Wiki-Agent` label (default client ID) as attribution.
Writes have a sliding budget of 60 attempts per subject per minute per instance.
The current v2 deployment has one instance; distributed deployment requires a
shared limiter before advertising the same global budget.

Diagram writes require JSON source plus caller-rendered HTML and PNG. The server
validates sizes, JSON shape and PNG signature, stores scoped attachments and
never executes rendering code. Attachment responses carry a sandbox CSP.

## Verification

Local tests use a mocked verified-token decoder and disposable PostgreSQL. They
exercise the real security filter chain, transport, input validation, permissions,
native writes, version attribution, conflicts and read-only ownership. They do
not establish real Keycloak/PKCE interoperability; record that acceptance with
personal accounts after the SSO configuration and runtime gates are satisfied.

Protocol references:
[HTTP transport](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports),
[authorization](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization),
[tool messages](https://modelcontextprotocol.io/specification/2025-11-25/server/tools),
[Keycloak administration](https://www.keycloak.org/docs/latest/server_admin/index.html).
