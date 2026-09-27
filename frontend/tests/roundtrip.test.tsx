import { cleanup, render } from '@testing-library/react';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { MarkdownEditor, type MarkdownEditorHandle } from '../src/features/edit/MarkdownEditor';
import { minimalDiff } from '../src/features/edit/minimalDiff';

const DIR = join(import.meta.dirname, 'fixtures/md');
const FILES = readdirSync(DIR)
  .filter(f => f.endsWith('.md'))
  .sort();

async function exportOf(input: string): Promise<{ out: string; changed: number }> {
  const ref = createRef<MarkdownEditorHandle>();
  let changed = 0;
  render(<MarkdownEditor ref={ref} value={input} onChange={() => changed++} onUploadImage={async () => ''} />);
  // let Crepe boot and settle; read twice to catch late normalization
  await new Promise(resolve => setTimeout(resolve, 1800));
  const first = ref.current?.getMarkdown() ?? '';
  await new Promise(resolve => setTimeout(resolve, 700));
  const second = ref.current?.getMarkdown() ?? '';
  cleanup();
  return { out: second, changed };
}

// T01 acceptance (refined by assets/editor-spike.md S5.2): the real MarkdownEditor
// must be idempotent (f(f(x)) == f(x)) on all 20 samples, and untouched open-save
// must restore original bytes through minimalDiff.
// (CodeMirror views need the IntersectionObserver stub in test-setup.ts.)
describe('MarkdownEditor roundtrip (20 samples)', () => {
  it.each(FILES)('%s is idempotent', async file => {
    const input = readFileSync(join(DIR, file), 'utf8');
    const first = await exportOf(input);
    const second = await exportOf(first.out);
    console.log(`${file}: zeroDiff=${first.out === input} stable=${first.out === second.out} onChange=${first.changed}`);
    expect(second.out, `${file}: second export must equal first (idempotent)`).toBe(first.out);
    // end-to-end T01 acceptance: untouched open-save produces zero diff via minimalDiff
    expect(minimalDiff(input, first.out), `${file}: minimalDiff must restore original bytes`).toBe(input);
  }, 60000);
});
