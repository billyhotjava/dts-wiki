# Content tools

## Markdown formatting

`tools/mdfmt [--check] <files-or-directories...>` explicitly normalizes Markdown;
`--stdin` reads one document and writes its formatted result. Node 24 and the
locked frontend dependencies are required. `--check` never writes: exit 0 means
all files are normalized, 1 means formatting is needed and 2 means an input or
tool error. Symlinks, invalid UTF-8 and documents larger than 10 MiB are rejected.

The CLI and Milkdown share `markdownFormatOptions`: `-` bullets, numbered lists,
one-space list marker indentation, `*` emphasis, fenced code and `---` rules.
GFM, math and directives are supported. Frontmatter stays outside body parsing;
GitHub callout markers stay readable. Wiki import never runs this formatter.
Normal editor saves still use the existing minimal-diff policy for untouched
blocks. The CLI's direct `remark-stringify:11.0.0` dependency was already present
in the locked editor dependency graph; its license is MIT. No version was upgraded.

## Diagram bundles

Install the reviewed MIT-licensed Archify 2.16 release outside the repository:

```sh
ARCHIFY_HOME=/path/to/archify tools/archify-build diagrams/example.archify.json
```

The wrapper validates, delivers and runs desktop containment/screenshots. It
requires nine successful showcase checks with zero errors/warnings, verifies
the frozen source/HTML digests and atomically copies the light 1440x900 PNG.
Keep `<name>.archify.json`, `<name>.html` and `<name>.png` together. Generated
visual-check sidecars are ignored. A successful automated visual receipt still
requires human screenshot review; the wrapper reports that status truthfully.

Reference the bundle in Markdown:

````markdown
```archify src="./diagrams/example.archify.json" height="560"
![Example diagram](diagrams/example.png)
```
````

Reading uses a same-origin authorized raw endpoint in an opaque sandbox that
allows scripts but has no `allow-same-origin`. CSP blocks external scripts,
network access and embedded remote content. HTML failure falls back to PNG;
missing files show an author-facing request to rebuild. The toolbar exposes
fullscreen, the sandboxed HTML, JSON source and PNG download. Raw document HTML
remains disabled. SVG uploads are rejected. JSON/HTML limits are 2 MB and 5 MB;
image uploads retain the configured 10 MiB limit.

MCP `wiki_put_diagram` accepts all three caller-built artifacts and stores stable
matching names on native pages. Replacing a bundle preserves attachment identity
and old content-addressed blobs. Git bundles arrive through inbound synchronization
and reject API writes. Attachment soft deletion hides metadata and retains bytes
for recovery; retention cleanup requires a separate reviewed policy.
