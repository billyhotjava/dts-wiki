import MarkdownIt from 'markdown-it';
import { useMemo } from 'react';

// W4 minimal renderer (design 05 S6 full stack — GFM plugins, Mermaid, Shiki,
// link rewriting, Anchor目录 — arrives with W5). Deliberately `html: false`:
// no raw HTML reaches the DOM, so no sanitizer is needed yet.
const md = new MarkdownIt({ breaks: true, html: false, linkify: true });

export function MarkdownView({ content }: { content: string }) {
  const html = useMemo(() => md.render(content), [content]);
  return <div className="markdown-body" dangerouslySetInnerHTML={{ __html: html }} />;
}
