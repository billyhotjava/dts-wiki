import { describe, expect, it } from 'vitest';
import { restoreCallouts } from '../src/features/edit/MarkdownEditor';

describe('restoreCallouts (E2 GitHub alerts)', () => {
  it('unescapes alert markers at blockquote starts', () => {
    expect(restoreCallouts('> \\[!NOTE]\n> text\n')).toBe('> [!NOTE]\n> text\n');
    expect(restoreCallouts('> \\[!WARNING] 看\n')).toBe('> [!WARNING] 看\n');
    expect(restoreCallouts('>> \\[!TIP]\n')).toBe('>> [!TIP]\n');
  });

  it('leaves ordinary escaped brackets alone', () => {
    expect(restoreCallouts('a \\[b\\] c\n')).toBe('a \\[b\\] c\n');
    expect(restoreCallouts('> \\[!UNKNOWN]\n')).toBe('> \\[!UNKNOWN]\n');
    expect(restoreCallouts('\\[!NOTE] not a quote\n')).toBe('\\[!NOTE] not a quote\n');
  });

  it('is idempotent', () => {
    const md = '> [!NOTE]\n> x\n';
    expect(restoreCallouts(restoreCallouts(md))).toBe(md);
  });
});
