import remarkDirective from 'remark-directive';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import remarkParse from 'remark-parse';
import remarkStringify, { type Options } from 'remark-stringify';
import { unified } from 'unified';
import { splitFrontmatter } from './frontmatter.ts';

/** Shared serialization rules for the editor and the opt-in CLI formatter. */
export const markdownFormatOptions = {
  bullet: '-', listItemIndent: 'one', emphasis: '*', strong: '*',
  rule: '-', ruleRepetition: 3, fences: true, incrementListMarker: true,
} as const satisfies Options;

export function restoreCallouts(md: string): string {
  return md.replace(/^((?:> ?)+)\\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]/gm, '$1[!$2]');
}

const processor = unified().use(remarkParse).use(remarkGfm).use(remarkDirective)
  .use(remarkMath).use(remarkStringify, markdownFormatOptions);

export function formatMarkdown(content: string): string {
  const { front, body } = splitFrontmatter(content.replace(/\r\n/g, '\n'));
  return front + restoreCallouts(String(processor.processSync(body)));
}
