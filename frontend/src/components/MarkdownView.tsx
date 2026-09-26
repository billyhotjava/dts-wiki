import MarkdownIt from 'markdown-it';
import anchor from 'markdown-it-anchor';
import container from 'markdown-it-container';
import footnote from 'markdown-it-footnote';
import githubAlerts from 'markdown-it-github-alerts';
import taskLists from 'markdown-it-task-lists';
import GithubSlugger from 'github-slugger';
import { useEffect, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../api/client';

function splitFrontmatter(md: string): string {
  if (!md.startsWith('---\n')) return md;
  const end = md.indexOf('\n---', 4);
  if (end < 0) return md;
  const after = md.slice(end + 4);
  return after.startsWith('\n') ? after.slice(1) : after;
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Full reading stack (design 10 S4.4, minus archify which arrives in W6.5):
// markdown-it (html:false, breaks:true, linkify) + footnote + details/columns
// containers + GitHub alerts + anchor (github-slugger) + task lists; mermaid,
// KaTeX and Shiki load lazily only when the page needs them.
function buildRenderer(): MarkdownIt {
  const slugger = new GithubSlugger();
  const md = new MarkdownIt({ breaks: true, html: false, linkify: true });
  md.use(footnote);
  for (const name of ['details', 'columns', 'column']) {
    md.use(container, name);
  }
  md.use(githubAlerts);
  md.use(anchor, { slugify: (s: string) => slugger.slug(s) });
  md.use(taskLists, { enabled: true });
  md.renderer.rules.fence = (tokens, idx) => {
    const token = tokens[idx];
    const info = token.info.trim().split(/\s+/)[0];
    if (info === 'mermaid') {
      return `<div class="wiki-mermaid" data-mermaid="${encodeURIComponent(token.content)}"></div>`;
    }
    if (info === 'archify') {
      // W6.5 renders the interactive frame; until then keep the source readable.
      return `<pre class="wiki-archify-src"><code>${escapeHtml(token.content)}</code></pre>`;
    }
    return `<pre data-shiki data-lang="${escapeHtml(info)}"><code>${escapeHtml(token.content)}</code></pre>`;
  };
  return md;
}

let sharedRenderer: MarkdownIt | null = null;
function renderer(): MarkdownIt {
  if (sharedRenderer === null) sharedRenderer = buildRenderer();
  return sharedRenderer;
}

export function MarkdownView({ content, pageId, spaceSlug }: { content: string; pageId: number; spaceSlug: string }) {
  const navigate = useNavigate();
  const hostRef = useRef<HTMLDivElement>(null);
  const body = useMemo(() => splitFrontmatter(content), [content]);
  const html = useMemo(() => renderer().render(body), [body]);

  useEffect(() => {
    const host = hostRef.current;
    if (host === null) return;
    let cancelled = false;

    // images lazy (native); external links open in a new tab
    host.querySelectorAll('img').forEach(img => img.setAttribute('loading', 'lazy'));
    host.querySelectorAll<HTMLAnchorElement>('a[href]').forEach(a => {
      const href = a.getAttribute('href') ?? '';
      if (/^(https?:|mailto:)/.test(href)) {
        a.setAttribute('target', '_blank');
        a.setAttribute('rel', 'noopener noreferrer');
      }
    });

    // relative .md links -> resolve-batch -> in-app navigation
    const pending: { a: HTMLAnchorElement; path: string }[] = [];
    host.querySelectorAll<HTMLAnchorElement>('a[href]').forEach(a => {
      const href = a.getAttribute('href') ?? '';
      if (href.endsWith('.md') && !/^(https?:|mailto:|#)/.test(href)) {
        pending.push({ a, path: href });
      }
    });
    if (pending.length > 0) {
      void api
        .post<Record<string, number | null>>(`/api/wiki/spaces/${spaceSlug}/resolve-batch`, { paths: pending.map(p => p.path) })
        .then(({ data }) => {
          if (cancelled) return;
          for (const { a, path } of pending) {
            const id = data[path];
            if (id !== null && id !== undefined) {
              a.addEventListener('click', e => {
                e.preventDefault();
                navigate(`/s/${spaceSlug}/p/${id}`);
              });
              a.setAttribute('href', `/s/${spaceSlug}/p/${id}`);
            }
          }
        })
        .catch(() => undefined);
    }

    // ./assets/* and ./diagrams/* -> raw download URLs
    host.querySelectorAll('img[src^="./"]').forEach(img => {
      const value = img.getAttribute('src') ?? '';
      img.setAttribute('src', `/api/wiki/pages/${pageId}/raw/${value.replace(/^\.\//, '')}`);
    });
    host.querySelectorAll('a[href^="./"]').forEach(a => {
      const value = a.getAttribute('href') ?? '';
      a.setAttribute('href', `/api/wiki/pages/${pageId}/raw/${value.replace(/^\.\//, '')}`);
    });

    // mermaid lazy
    const mermaids = [...host.querySelectorAll('[data-mermaid]')];
    if (mermaids.length > 0) {
      void import('mermaid')
        .then(({ default: mermaid }) => {
          if (cancelled) return;
          mermaid.initialize({ startOnLoad: false, theme: 'default' });
          return Promise.all(
            mermaids.map(async (el, i) => {
              try {
                const code = decodeURIComponent(el.getAttribute('data-mermaid') ?? '');
                const { svg } = await mermaid.render(`wiki-m-${pageId}-${i}`, code);
                if (!cancelled) el.innerHTML = svg;
              } catch {
                if (!cancelled) el.innerHTML = `<pre><code>${escapeHtml(decodeURIComponent(el.getAttribute('data-mermaid') ?? ''))}</code></pre>`;
              }
            }),
          );
        })
        .catch(() => undefined);
    }

    // KaTeX only when the page contains math delimiters (CSS loads with the renderer)
    if (/\$\$.+\$\$|\$[^$\n]+\$/.test(body)) {
      void Promise.all([import('katex/dist/katex.min.css'), import('katex/dist/contrib/auto-render')])
        .then(([, { default: renderMath }]) => {
          if (!cancelled) renderMath(host, { delimiters: [{ left: '$$', right: '$$', display: true }, { left: '$', right: '$', display: false }] });
        })
        .catch(() => undefined);
    }

    // Shiki highlight with plain <pre> fallback
    const blocks = [...host.querySelectorAll('pre[data-shiki]')];
    if (blocks.length > 0) {
      void import('shiki')
        .then(async ({ createHighlighter }) => {
          const highlighter = await createHighlighter({ themes: ['github-light'], langs: ['js', 'ts', 'java', 'python', 'bash', 'yaml', 'json', 'markdown', 'xml', 'sql'] });
          if (cancelled) return;
          for (const el of blocks) {
            try {
              const code = el.querySelector('code');
              const lang = el.getAttribute('data-lang') ?? '';
              const text = code?.textContent ?? '';
              if (text === '') continue;
              el.outerHTML = highlighter.codeToHtml(text, { lang: lang === '' ? 'text' : lang, theme: 'github-light' });
            } catch {
              // keep plain <pre> text
            }
          }
        })
        .catch(() => undefined);
    }

    // code copy buttons
    host.querySelectorAll('pre').forEach(pre => {
      if (pre.querySelector('.wiki-copy') !== null) return;
      const button = document.createElement('button');
      button.className = 'wiki-copy';
      button.textContent = '复制';
      button.onclick = () => {
        void navigator.clipboard?.writeText(pre.innerText.replace(/^复制/, '')).catch(() => undefined);
      };
      pre.style.position = 'relative';
      pre.appendChild(button);
    });

    return () => {
      cancelled = true;
    };
  }, [html, body, navigate, pageId, spaceSlug]);

  return <div ref={hostRef} className="markdown-body" dangerouslySetInnerHTML={{ __html: html }} />;
}
