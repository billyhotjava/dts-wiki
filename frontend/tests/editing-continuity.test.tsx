import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EditingContinuity } from '../src/features/edit/EditingContinuity';
import { minimalDiff } from '../src/features/edit/minimalDiff';
import '../src/i18n';

afterEach(async () => { await act(async () => cleanup()); vi.useRealTimers(); vi.unstubAllGlobals(); });
describe('Editing continuity', () => {
  it('recovers a private draft with its original base and shows other editors', async () => {
    const recover = vi.fn();
    vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => Promise.resolve(Response.json(init?.method === 'POST' ? [{ displayName: 'Alice' }] : { contentMd: 'Draft', baseVersionNo: 2, updatedAt: '2026-10-06' }))));
    render(<EditingContinuity pageId={7} enabled baseVersionNo={3} initialMarkdown="Published" getContent={() => 'Edited'} onRecover={recover} />);
    await screen.findByText('Alice 正在编辑此页');
    fireEvent.click(await screen.findByRole('button', { name: '恢复草稿' }));
    expect(recover).toHaveBeenCalledWith(expect.objectContaining({ contentMd: 'Draft', baseVersionNo: 2 }));
  });
  it('autosaves edits with the frozen base and releases presence on unmount', async () => {
    vi.useFakeTimers(); const calls: { url: string; init?: RequestInit }[] = [];
    vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => { calls.push({ url, init }); return Promise.resolve(init?.method === 'POST' ? Response.json([]) : new Response(null, { status: 204 })); }));
    const view = render(<EditingContinuity pageId={7} enabled baseVersionNo={2} initialMarkdown="Published" getContent={() => 'Edited'} onRecover={vi.fn()} />);
    await act(async () => { await vi.advanceTimersByTimeAsync(10000); });
    const draft = calls.find(call => call.url.endsWith('/draft') && call.init?.method === 'PUT');
    expect(JSON.parse(String(draft?.init?.body))).toEqual({ contentMd: 'Edited', baseVersionNo: 2 });
    await act(async () => view.unmount());
    expect(calls.some(call => call.url.endsWith('/editing') && call.init?.method === 'DELETE')).toBe(true);
  });
  it('does not open a draft or presence for a read-only page', () => {
    const fetch = vi.fn(); vi.stubGlobal('fetch', fetch);
    render(<EditingContinuity pageId={7} enabled={false} baseVersionNo={2} initialMarkdown="Published" getContent={() => 'Edited'} onRecover={vi.fn()} />);
    expect(fetch).not.toHaveBeenCalled();
  });
  it('preserves surrounding bytes in a large document with a changed block', () => {
    const original = Array.from({ length: 3000 }, (_, index) => `Paragraph ${index}  original spacing.\n\n`).join('');
    const edited = original.replace('Paragraph 1500  original spacing.', 'Paragraph 1500 changed.');
    expect(minimalDiff(original, edited)).toBe(edited);
  });
  it('retains distant unchanged regions when many blocks are replaced', () => {
    const original = Array.from({ length: 900 }, (_, index) => `Paragraph ${index} original.\n\n`).join('');
    const edited = Array.from({ length: 900 }, (_, index) => `${index % 7 === 0 ? 'Changed' : 'Paragraph'} ${index} original.\n\n`).join('');
    expect(minimalDiff(original, edited)).toBe(edited);
  });
});
