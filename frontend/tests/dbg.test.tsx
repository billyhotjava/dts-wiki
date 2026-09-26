import { cleanup, render } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { MarkdownEditor, type MarkdownEditorHandle } from '../src/features/edit/MarkdownEditor';

async function exportOf(input: string): Promise<string> {
  const ref = createRef<MarkdownEditorHandle>();
  render(<MarkdownEditor ref={ref} value={input} onChange={() => {}} onUploadImage={async () => ''} />);
  await new Promise(r => setTimeout(r, 2500));
  const out = ref.current?.getMarkdown() ?? '';
  cleanup();
  return out;
}

describe('dbg', () => {
  it('sample-20 diff', async () => {
    const input = readFileSync('/opt/prod/dts/dts-rdc/dts-wiki/frontend/tests/roundtrip-samples/sample-20.md', 'utf8');
    const v1 = await exportOf(input);
    const v2 = await exportOf(v1);
    const a = v1.split('\n'), b = v2.split('\n');
    console.log('V1==V2:', v1 === v2);
    for (let i = 0; i < Math.max(a.length, b.length); i++) {
      if (a[i] !== b[i]) console.log(`L${i}: ${JSON.stringify(a[i])} => ${JSON.stringify(b[i])}`);
    }
  }, 60000);
});
