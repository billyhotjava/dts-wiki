import { cleanup, render } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { MarkdownEditor, type MarkdownEditorHandle } from '../src/features/edit/MarkdownEditor';

async function exportOf(input: string): Promise<string> {
  const ref = createRef<MarkdownEditorHandle>();
  render(<MarkdownEditor ref={ref} value={input} onChange={() => undefined} onUploadImage={async () => ''} />);
  await new Promise(resolve => setTimeout(resolve, 2200));
  const out = ref.current?.getMarkdown() ?? '';
  cleanup();
  return out;
}

// E6/E8/math preservation probe (09 S2.4): unknown constructs must survive untouched.
describe('dialect preservation', () => {
  it('columns directives pass through', async () => {
    const input = ':::columns\n:::column\n左\n:::\n:::column\n右\n:::\n:::\n';
    expect(await exportOf(input)).toBe(input);
  }, 30000);

  it('math stays as typed', async () => {
    const input = '公式 $E=mc^2$ 和块：\n\n$$\n\\int_0^1 x dx\n$$\n';
    const out = await exportOf(input);
    expect(out).toContain('E=mc^2');
    expect(out).toContain('\\int_0^1 x dx');
  }, 30000);
});
