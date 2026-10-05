import { beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { Highlight } from '../src/features/search/Highlight';
import { lineDiff } from '../src/features/history/lineDiff';
import { HistoryTab } from '../src/features/history/HistoryTab';
import { useRestoreVersion, useVersion, useVersions, type PageView } from '../src/api/hooks';
import '../src/i18n';

vi.mock('../src/api/hooks', () => ({ useRestoreVersion: vi.fn(), useVersion: vi.fn(), useVersions: vi.fn() }));
vi.mock('../src/components/MarkdownView', () => ({ MarkdownView: () => <div /> }));

describe('Search and history', () => {
  beforeEach(() => {
    cleanup();
    vi.mocked(useVersions).mockReturnValue({ data: { items: [{ versionNo: 1, authorName: 'alice', viaAgent: 'personal-agent', source: 'WEB', message: '', createdAt: '2026-10-05T06:00:00Z' }], total: 1 }, isLoading: false, isError: false, refetch: vi.fn() } as unknown as ReturnType<typeof useVersions>);
    vi.mocked(useVersion).mockReturnValue({ data: { contentMd: 'Version one' }, isLoading: false, isError: false, refetch: vi.fn() } as unknown as ReturnType<typeof useVersion>);
    vi.mocked(useRestoreVersion).mockReturnValue({ mutateAsync: vi.fn(), isPending: false } as unknown as ReturnType<typeof useRestoreVersion>);
  });

  it('keeps search content as escaped text and treats punctuation literally', () => {
    const { container } = render(<Highlight text={'<img src=x onerror=alert(1)> a+b 中文'} term="a+b" />);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('mark')?.textContent).toBe('a+b');
  });

  it('reconstructs both versions from an insertion and deletion diff', () => {
    const before = 'start\nold\nunchanged\nend', after = 'start\nnew\nunchanged\nmore\nend';
    const diff = lineDiff(before, after);
    expect(diff.filter(l => l.kind !== 'add').map(l => l.text).join('\n')).toBe(before);
    expect(diff.filter(l => l.kind !== 'remove').map(l => l.text).join('\n')).toBe(after);
    expect(diff.some(l => l.kind === 'remove' && l.text === 'old')).toBe(true);
    expect(diff.some(l => l.kind === 'same' && l.text === 'unchanged')).toBe(true);
  });

  it('bounds comparison work for large versions while retaining correct text', () => {
    const before = 'same\n' + Array.from({ length: 1000 }, (_, i) => `old ${i}`).join('\n') + '\nend';
    const after = before.replaceAll('old ', 'new ');
    const diff = lineDiff(before, after);
    expect(diff.filter(l => l.kind !== 'add').map(l => l.text).join('\n')).toBe(before);
    expect(diff.filter(l => l.kind !== 'remove').map(l => l.text).join('\n')).toBe(after);
  });

  it('shows agent attribution and hides restore for Git history', () => {
    render(<MemoryRouter><HistoryTab page={{ id: 1, versionNo: 2, spaceSlug: 'team-notes', editable: true, gitReadOnly: true } as PageView} /></MemoryRouter>);
    expect(screen.getByText('alice（经 personal-agent）')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: '对比版本' }));
    expect(screen.queryByRole('button', { name: '恢复为版本 1' })).toBeNull();
  });

  it('offers restore for an editable native page', async () => {
    render(<MemoryRouter><HistoryTab page={{ id: 1, versionNo: 2, spaceSlug: 'team-notes', editable: true, gitReadOnly: false } as PageView} /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: '对比版本' }));
    await waitFor(() => expect(screen.getByRole('button', { name: '恢复为版本 1' })).toBeVisible());
  });
});
