import { describe, expect, it } from 'vitest';
import { renderDocument } from './components/MarkdownView';

describe('document outline and stable deep links', () => {
  it('keeps duplicate headings unique inside each document and stable across visits', () => {
    const body = '# Intro\n\n## Details\n\n## Details\n';
    const first = renderDocument(body);
    renderDocument('# Details\n\n# Other\n');
    expect(renderDocument(body)).toEqual(first);
    expect(first.headings.map(heading => heading.id)).toEqual(['intro', 'details', 'details-1']);
  });
  it('uses readable Unicode and formatted titles without interpreting raw HTML', () => {
    const document = renderDocument('# 公司文档\n\n## **Bold** and `code`\n\n<script>unsafe()</script>');
    expect(document.headings.map(heading => heading.title)).toEqual(['公司文档', 'Bold and code']);
    expect(document.html).not.toContain('<script>');
  });
  it('excludes heading-like fenced code from navigation', () => {
    const document = renderDocument('## Heading\n\n```text\n# Not a heading\n```');
    expect(document.headings).toHaveLength(1);
  });
});
