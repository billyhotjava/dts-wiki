import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { PersonalActions } from '../src/features/collaboration/PersonalActions';
import { CommentsTab } from '../src/features/collaboration/CommentsTab';
import { NotificationsBell } from '../src/features/collaboration/NotificationsBell';
import '../src/i18n';

afterEach(async () => { await act(async () => cleanup()); vi.unstubAllGlobals(); });
function show(component: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}><MemoryRouter>{component}</MemoryRouter></QueryClientProvider>);
}
function fixture(handler: (path: string, init?: RequestInit) => Response) {
  vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => Promise.resolve(handler(new URL(url, 'http://localhost').pathname, init))));
}
describe('Wiki collaboration', () => {
  it('toggles personal favorite and watch without writing content', async () => {
    let favorite = false, watching = false; const writes: string[] = [];
    fixture((path, init) => {
      if (init?.method !== 'GET') { writes.push(path); if (path.includes('/favorites/')) favorite = !favorite; else watching = !watching; return new Response(null, { status: 204 }); }
      return Response.json({ favorite, watching });
    });
    show(<PersonalActions pageId={7} />);
    await waitFor(() => expect(screen.getByRole('button', { name: /收\s*藏/ })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /收\s*藏/ }));
    await screen.findByRole('button', { name: '取消收藏' });
    fireEvent.click(screen.getByRole('button', { name: /关\s*注/ }));
    await screen.findByRole('button', { name: '取消关注' });
    expect(writes).toEqual(['/api/wiki/me/favorites/7', '/api/wiki/pages/7/watch']);
  });
  it('keeps comment HTML inert and enforces reply ownership in the UI', async () => {
    fixture(() => Response.json({ total: 1, items: [{ id: 2, parentId: null, bodyMd: '<script>window.unsafe=true</script>\nHello', authorLogin: 'reader', authorName: 'Reader', createdAt: '2026-10-06', deleted: false, resolved: false, canEdit: false, replies: [] }] }));
    const { container } = show(<CommentsTab pageId={7} spaceSlug="team" />);
    await screen.findByText('Reader');
    expect(container.querySelector('script')).toBeNull();
    expect(screen.queryByRole('button', { name: /编\s*辑/ })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: /回\s*复/ }));
    expect(screen.getByText('正在回复评论')).toBeInTheDocument();
  });
  it('posts plain Markdown with an explicit root reply id', async () => {
    let posted: unknown;
    fixture((_path, init) => { if (init?.method === 'POST') { posted = JSON.parse(String(init.body)); return Response.json({}); } return Response.json({ items: [], total: 0 }); });
    show(<CommentsTab pageId={7} spaceSlug="team" />);
    fireEvent.change(screen.getByPlaceholderText('写下评论，支持 Markdown'), { target: { value: 'Hello @reader' } });
    fireEvent.click(screen.getByRole('button', { name: '发表评论' }));
    await waitFor(() => expect(posted).toEqual({ bodyMd: 'Hello @reader', parentId: null }));
  });
  it('shows identity outage instead of stale unread counts', async () => {
    fixture(() => Response.json({ errorKey: 'IDENTITY_UNAVAILABLE' }, { status: 503 }));
    show(<NotificationsBell />);
    fireEvent.click(screen.getByRole('button', { name: '通知' }));
    await screen.findByText('暂时无法确认当前访问权限，请稍后重试');
    expect(screen.getByRole('button', { name: '全部标记已读' })).toBeDisabled();
  });
});
