import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PageView } from '../src/features/page/PageView';
import { PageTree } from '../src/features/tree/PageTree';
import { PageEditorPage } from '../src/features/edit/PageEditorPage';
import type { PageView as PageData, TreeNode } from '../src/api/hooks';
import '../src/i18n';

const state = vi.hoisted(() => ({ page: null as unknown as PageData, tree: [] as TreeNode[], save: vi.fn() }));
vi.mock('../src/api/hooks', () => ({
  usePage: () => ({ data: state.page, isLoading: false, isError: false, refetch: vi.fn() }),
  useTree: () => ({ data: state.tree, refetch: vi.fn() }),
  useDeletePage: () => ({ mutateAsync: vi.fn() }),
  useRenameMove: () => ({ mutateAsync: vi.fn(), mutate: vi.fn() }),
  useCopyPage: () => ({ mutateAsync: vi.fn() }),
  useCreatePage: () => ({ mutateAsync: vi.fn() }),
  useTemplates: () => ({ data: [] }),
  useSavePageContent: () => ({ mutateAsync: state.save }),
}));
vi.mock('@tanstack/react-query', () => ({ useQueryClient: () => ({ prefetchQuery: vi.fn() }) }));
vi.mock('../src/components/MarkdownView', () => ({ MarkdownView: () => <div>Content</div> }));
vi.mock('../src/features/page/PropertiesPanel', () => ({ PropertiesPanel: () => null }));
vi.mock('../src/features/edit/AttachmentsTab', () => ({ AttachmentsTab: () => null }));
vi.mock('../src/features/edit/MarkdownEditor', () => ({ MarkdownEditor: () => <div>Editor</div> }));
vi.mock('../src/features/edit/SourceEditor', () => ({ SourceEditor: () => null }));
vi.mock('../src/features/edit/PropertiesForm', () => ({ PropertiesForm: () => null }));
vi.mock('../src/features/edit/MentionPicker', () => ({ MentionPicker: () => null }));
vi.mock('../src/features/edit/VersionConflictModal', () => ({ VersionConflictModal: () => null }));

function show(gitReadOnly: boolean) {
  state.page = {
    id: 1, spaceSlug: 'team-notes', title: 'Knowledge', kind: gitReadOnly ? 'GIT' : 'NATIVE',
    contentMd: '# Knowledge', versionNo: 1, updatedAt: '', updatedBy: null,
    gitPath: gitReadOnly ? 'docs/knowledge.md' : null, gitRepoUrl: null, gitCommit: null,
    syncStatus: 'SYNCED', breadcrumbs: [], labels: [], watching: false,
    editable: true, gitReadOnly, meta: null, url: '/s/team-notes/p/1',
  };
  render(<MemoryRouter initialEntries={['/s/team-notes/p/1']}><Routes><Route path="/s/:slug/p/:pageId" element={<PageView />} /></Routes></MemoryRouter>);
}

afterEach(() => { cleanup(); state.tree = []; vi.clearAllMocks(); });

describe('Git source ownership', () => {
  it('shows the Git read-only label and hides editing actions even if editable is stale', () => {
    show(true);
    expect(screen.getByText('来自 Git · 只读')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /编\s*辑/ })).not.toBeInTheDocument();
  });
  it('keeps editing available for native content', () => {
    show(false);
    expect(screen.queryByText('来自 Git · 只读')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /编\s*辑/ })).toBeEnabled();
  });

  it('offers no context-menu mutations for a Git tree node', () => {
    state.tree = [{ id: 1, title: 'Git note', kind: 'GIT', readOnly: true, children: [], hasChildren: false, syncStatus: 'SYNCED' }];
    render(<MemoryRouter initialEntries={['/s/team-notes/p/1']}><Routes><Route path="/s/:slug/p/:pageId" element={<PageTree />} /></Routes></MemoryRouter>);
    fireEvent.contextMenu(screen.getByText('Git note'));
    expect(screen.getByTitle('来自 Git · 只读')).toBeInTheDocument();
    expect(screen.queryByText('新建子页')).not.toBeInTheDocument();
    expect(screen.queryByText('改名')).not.toBeInTheDocument();
  });

  it('protects direct edit routes and ignores the save shortcut for Git pages', () => {
    show(true);
    cleanup();
    render(<MemoryRouter initialEntries={['/s/team-notes/p/1/edit']}><Routes><Route path="/s/:slug/p/:pageId/edit" element={<PageEditorPage mode="edit" />} /></Routes></MemoryRouter>);
    expect(screen.getByText('请在源仓库中修改此页面。')).toBeInTheDocument();
    expect(screen.queryByText('Editor')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /保\s*存/ })).not.toBeInTheDocument();
    fireEvent.keyDown(window, { key: 's', ctrlKey: true });
    expect(state.save).not.toHaveBeenCalled();
  });
});
