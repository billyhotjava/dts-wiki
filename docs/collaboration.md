# Personal navigation and collaboration

Favorites and the fifty most recent visits belong to the authenticated user.
Views use server time and serialize upsert/pruning per user. Lists count and
paginate only live pages in currently visible spaces. Tags continue to come
from frontmatter; there is no second editable label store.

Comments are Wiki-owned, including on Git-managed pages. Readers can comment;
the author or a Wiki administrator can edit, resolve or soft-delete a comment.
A thread has one reply level and at most 200 replies. Root comments with live
replies retain an empty tombstone after deletion. Markdown HTML is disabled.
Comments never alter Git Markdown, page versions or synchronization outboxes.

Saving or commenting automatically watches a page unless the user explicitly
unwatched it. Unwatch stores a persistent mute; manual watch removes the mute.
Mention notifications are independent of watcher mute. Repeated mentions within
one event create one intent for each recipient/type. Mentions ignore email
addresses, inline code and fenced code and only resolve local identity profiles.

## External identity configuration

The local profile is display data, not recipient authorization. Configure
`application.wiki.directory` from the instance's external configuration:

| Setting | Meaning |
| --- | --- |
| `base-url` | HTTPS Keycloak base URL; localhost HTTP is for tests only |
| `realm` | Externally managed company or product realm |
| `wiki-client-uuid` | Internal Keycloak UUID of the Wiki client |
| `client-id`, `client-secret` | Dedicated read-only service credential |

Infra owns identity-client provisioning. Grant only the administrative read
permissions needed to inspect enabled users and the Wiki client's effective
client-role mappings. Validate this least-privilege grant against the deployed
Keycloak version before enabling the adapter. Do not grant user/role mutation,
assign groups, restart SSO or reuse an administrative password. No cross-database
access is used. The credential token is cached briefly; recipient roles are
read again on every check, including inherited/composite roles. Missing settings
or an identity outage produces 503 `IDENTITY_UNAVAILABLE`; a stale local role
snapshot is never used as a fallback. Empty mention searches return no users;
queries inspect at most twenty local display candidates and disclose no email.

## Notification dispatch

Content transactions record idempotent event/recipient/type intent. A worker
checks enabled identity and current page/space permission outside the transaction.
Revoked, disabled or deleted targets are suppressed. Identity outages preserve
pending intent and retry after one minute. List, unread count and opening a
notification check current permissions again. The bell polls every sixty seconds.

SMTP is disabled by default. Configure `application.wiki.notifications` with
`mail-enabled`, `from` and an HTTPS `public-url`, plus external `spring.mail`
settings. The Compose template exposes corresponding `WIKI_MAIL_*` variables.
Only verified current email addresses receive mail. Connection/read/write budgets
default to 2/5/5 seconds. Page updates are combined by recipient/page within ten
minutes; in-app read state does not change email delivery state. Sending uses
short database claims and no database transaction during SMTP. Failed delivery
retries up to eight attempts with exponential delay capped at sixty minutes;
expired sending claims are recovered after five minutes.

SMTP acknowledgement is not atomic with a database commit. An ambiguous send or
process crash after acceptance can duplicate an email on retry. Event intent is
idempotent; SMTP delivery is at least once. This is not an exactly-once guarantee.

## Operations

Watch `wiki_notification_identity_unavailable` and `wiki_notification_mail_retry`
logs; they contain notification IDs, never tokens or message bodies. Alert when
the oldest pending intent exceeds fifteen minutes, identity failures persist for
five minutes, or any delivery reaches `FAILED`. Diagnose external permissions,
TLS/SMTP settings and dependency health before retrying. Requeue only reviewed
failed IDs; preserve their event keys. Include personal tables, comments, watches
and notification intent in the same Wiki database backup. Migration `9008` only
adds tables/columns/indexes; historical migration checksums are unchanged.

Local integration tests use disposable PostgreSQL and fake identity/SMTP.
Real company identity grants, disabled/revoked accounts and SMTP delivery remain
separate deployment acceptance checks.
