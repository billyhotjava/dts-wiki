import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { minimalDiff } from '../src/features/edit/minimalDiff';

const DIR = join(import.meta.dirname, 'fixtures/md');
const FILES = readdirSync(DIR)
  .filter(f => f.endsWith('.md'))
  .sort();

function changedLines(a: string, b: string): number {
  const al = a.split('\n');
  const bl = b.split('\n');
  let s = 0;
  while (s < Math.min(al.length, bl.length) && al[s] === bl[s]) s++;
  let e = 0;
  while (e < Math.min(al.length - s, bl.length - s) && al[al.length - 1 - e] === bl[bl.length - 1 - e]) e++;
  return Math.max(al.length - s - e, bl.length - s - e);
}

describe('minimalDiff', () => {
  it.each(FILES)('%s unchanged round-trips byte-identical', file => {
    const original = readFileSync(join(DIR, file), 'utf8');
    expect(minimalDiff(original, original)).toBe(original);
  });

  it('one changed word touches one line', () => {
    const original = '# Title\n\nFirst paragraph here.\n\nSecond paragraph here.\n';
    const edited = '# Title\n\nFirst paragraph HERE.\n\nSecond paragraph here.\n';
    expect(changedLines(original, minimalDiff(original, edited))).toBe(1);
  });

  it('one table cell change keeps other blocks byte-identical', () => {
    const original = '# T\n\nSome intro text.\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\nTail text.\n';
    const edited = '# T\n\nSome intro text.\n\n| a | b |\n|---|---|\n| 1 | 99 |\n\nTail text.\n';
    const out = minimalDiff(original, edited);
    expect(out).toContain('99');
    expect(out.startsWith('# T\n\nSome intro text.\n')).toBe(true);
    expect(out.endsWith('\nTail text.\n')).toBe(true);
  });

  it('add list item, delete heading, reorder paragraphs touch only involved blocks', () => {
    const original = '# A\n\n- one\n- two\n\nPara one.\n\nPara two.\n';
    const added = '# A\n\n- one\n- two\n- three\n\nPara one.\n\nPara two.\n';
    expect(minimalDiff(original, added)).toBe(added);
    const deleted = 'Para one.\n\nPara two.\n';
    expect(minimalDiff(original, deleted)).toBe(deleted);
    const reordered = '# A\n\n- one\n- two\n\nPara two.\n\nPara one.\n';
    const out = minimalDiff(original, reordered);
    expect(out).toContain('Para two.\n\nPara one.');
  });

  it('is idempotent on identical inputs', () => {
    const x = '# A\n\ntext\n';
    expect(minimalDiff(x, x)).toBe(x);
  });

  it('uneven table rows filled by the editor keep original bytes', () => {
    const original = '| h1 | h2 | h3 |\n|---|---|---|\n| a | b |\n| c | d | e |\n';
    const edited = '| h1 | h2 | h3 |\n| --- | --- | --- |\n| a  | b  |    |\n| c  | d  | e  |\n';
    expect(minimalDiff(original, edited)).toBe(original);
  });

  it('list marker style changes keep original bytes', () => {
    const original = '* one\n* two\n';
    const edited = '- one\n- two\n';
    expect(minimalDiff(original, edited)).toBe(original);
  });
});
