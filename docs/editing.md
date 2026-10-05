# Editing continuity

Native pages use optimistic version checks. Opening an editor freezes the page's
base version and original Markdown. Background query refreshes do not replace
unsaved text or silently advance that base. A successful save may update the
title and Markdown atomically. Identical Markdown creates no additional body
version. Native create/save content is limited to2 MB UTF-8; titles have at most
200 characters. A conflict offers an explicit reload or a second confirmed overwrite.

The editor saves changed Markdown to a private database draft every ten seconds.
Drafts retain the original base version, including when recovered after another
author publishes. Recovery switches to source editing and publishes only on an
explicit save. Discard removes only the caller's draft. Publishing clears a
matching draft in the same transaction; a delayed autosave of the published text
does not recreate it. Drafts are limited to 2 MB UTF-8. Draft title changes are
not persisted until publishing. A new page needs an ID before draft persistence
starts; uploading an attachment creates that page first.

Soft presence renews every thirty seconds and expires after ninety seconds.
It shows at most fifty other editors and creates no editing lock. Presence is
ephemeral and limited to ten thousand entries per instance. This is a single
instance feature; multiple replicas would require a shared presence store.
Leaving the editor releases presence when possible. Both draft and presence
endpoints require native-page write permission; Git pages remain read-only.

Visual editing preserves unchanged Markdown blocks. The alignment computation
trims common edges and caps the LCS matrix at 500,000 cells. Larger changed
regions use a bounded look-ahead alignment. Source mode preserves supplied
Markdown verbatim.

Trash lists are scoped and paginated before serialization, with at most fifty
items. The view displays titles and deletion times. Restore remains protected by
the page and subtree write policy. The original ID-list API is retained for
compatibility; new clients use `/api/wiki/spaces/{slug}/trash/items`.
