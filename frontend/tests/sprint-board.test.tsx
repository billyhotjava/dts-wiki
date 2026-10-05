import { beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { useContentQuery } from '../src/api/hooks';
import { SprintBoardPage } from '../src/features/query/SprintBoardPage';
import '../src/i18n';

vi.mock('../src/api/hooks', () => ({ useContentQuery: vi.fn() }));
const mockedQuery = vi.mocked(useContentQuery);
const task = { pageId: 7, docId: 'S5/F3/T15', type: 'task', status: 'DONE', title: '已交付查询', owner: 'alice', priority: 'P0', sprint: 'S5', feature: 'F3', url: '/s/team-notes/p/7', gitPath: 'notes/task.md', valid: true, updatedAt: '' };

function show(path = '/s/team-notes/board') {
  return render(<MemoryRouter initialEntries={[path]}><Routes><Route path="/s/:slug/board" element={<SprintBoardPage />} /></Routes></MemoryRouter>);
}

describe('Sprint board', () => {
  beforeEach(() => {
    cleanup();
    mockedQuery.mockReturnValue({ data: { items: [task], total: 201 }, isLoading: false, isError: false, refetch: vi.fn() } as unknown as ReturnType<typeof useContentQuery>);
  });

  it('links tasks to their pages and exposes the full result count', () => {
    show();
    expect(screen.getByRole('link', { name: '已交付查询' })).toHaveAttribute('href', task.url);
    expect(screen.getByText('当前页 1 项，共 201 项')).toBeVisible();
    expect(mockedQuery).toHaveBeenLastCalledWith({ space: 'team-notes', type: 'task', page: '0', size: '200' });
  });

  it('uses URL filters and resets the result page after changing a filter', () => {
    show('/s/team-notes/board?sprint=S5&feature=F3&status=DONE&page=1');
    expect(mockedQuery).toHaveBeenLastCalledWith(expect.objectContaining({ sprint: 'S5', feature: 'F3', status: 'DONE', page: '1' }));
    fireEvent.change(screen.getByRole('textbox', { name: '负责人' }), { target: { value: 'alice' } });
    expect(mockedQuery).toHaveBeenLastCalledWith(expect.objectContaining({ sprint: 'S5', feature: 'F3', owner: 'alice', page: '0' }));
  });
});
