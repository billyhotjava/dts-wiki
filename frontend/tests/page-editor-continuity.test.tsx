import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PageEditorPage } from '../src/features/edit/PageEditorPage';
import { SpaceHomePage } from '../src/features/home/SpaceHomePage';
import '../src/i18n';

const state = vi.hoisted(() => ({ page: { id: 7, title: 'Original title', contentMd: 'Published', versionNo: 2, editable: true, gitReadOnly: false }, save: vi.fn(), create: vi.fn(), refetch: vi.fn() }));
vi.mock('antd', async importOriginal => ({ ...await importOriginal<typeof import('antd')>(), message: { success: vi.fn(), error: vi.fn() } }));
vi.mock('../src/api/hooks', () => ({
  usePage: () => ({ data: state.page, isLoading: false, isError: false, refetch: state.refetch }),
  useTemplates: () => ({ data: [] }),
  useCreatePage: () => ({ mutateAsync: state.create }),
  useSpace: () => ({ data: { name: 'Team', rootPageId: 7, editable: true }, isLoading: false, isError: false, refetch: vi.fn() }),
  useSavePageContent: () => ({ mutateAsync: state.save }),
}));
vi.mock('../src/features/edit/MarkdownEditor', async () => {
  const React = await import('react');
  return { MarkdownEditor: React.forwardRef(function Editor({ value }: { value: string }, ref) {
    const [text, setText] = React.useState(value);
    React.useImperativeHandle(ref, () => ({ getMarkdown: () => text }));
    return <textarea aria-label="editor" value={text} onChange={event => setText(event.target.value)} />;
  }) };
});
vi.mock('../src/features/edit/SourceEditor', async () => {
  const React = await import('react');
  return { SourceEditor: React.forwardRef(function Source({ value }: { value: string }, ref) {
    React.useImperativeHandle(ref, () => ({ getMarkdown: () => value }));
    return <textarea aria-label="source" value={value} readOnly />;
  }) };
});
vi.mock('../src/features/edit/EditingContinuity', () => ({ EditingContinuity: ({ onRecover }: { onRecover: (draft: unknown) => void }) =>
  <button onClick={() => onRecover({ contentMd: 'Recovered edits', baseVersionNo: 1, updatedAt: '' })}>Recover fixture</button> }));
vi.mock('../src/features/edit/VersionConflictModal', () => ({ VersionConflictModal: ({ open, onReload }: { open: boolean; onReload: () => void }) => open ? <button onClick={onReload}>Reload fixture</button> : null }));
vi.mock('../src/features/edit/PropertiesForm', () => ({ PropertiesForm: () => null }));
vi.mock('../src/features/edit/AttachmentsTab', () => ({ AttachmentsTab: () => null }));
vi.mock('../src/features/edit/MentionPicker', () => ({ MentionPicker: () => null }));
vi.mock('../src/features/history/ActivityPanel', () => ({ ActivityPanel: () => null }));

const editor = () => <MemoryRouter initialEntries={['/s/team-notes/p/7/edit']}><Routes>
  <Route path="/s/:slug/p/:pageId/edit" element={<PageEditorPage mode="edit" />} /><Route path="*" element={<div>Saved</div>} />
</Routes></MemoryRouter>;
afterEach(() => { cleanup(); vi.resetAllMocks(); state.page = { id: 7, title: 'Original title', contentMd: 'Published', versionNo: 2, editable: true, gitReadOnly: false }; });

describe('Page editor version continuity', () => {
  it('creates from the space home beneath the existing root rather than creating a second root', async () => {
    state.create.mockResolvedValue({ id: 8 });
    render(<MemoryRouter initialEntries={['/s/team-notes']}><Routes>
      <Route path="/s/:slug" element={<SpaceHomePage />} />
      <Route path="/s/:slug/new" element={<PageEditorPage mode="new" />} />
      <Route path="*" element={<div>Saved</div>} />
    </Routes></MemoryRouter>);
    fireEvent.click(await screen.findByRole('button', { name: /(新建|创建)/ }));
    fireEvent.change(await screen.findByLabelText('editor'), { target: { value: 'Child body' } });
    fireEvent.change(screen.getByPlaceholderText('标题'), { target: { value: 'Child title' } });
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(state.create).toHaveBeenCalledWith({ parentId: 7, title: 'Child title', contentMd: 'Child body' }));
  });
  it('keeps unsaved body, title and base during refresh and adopts the new base only after explicit reload', async () => {
    state.save.mockRejectedValueOnce({ response: { status: 409, data: { currentVersionNo: 3 } } }).mockResolvedValueOnce({ versionNo: 5 });
    const view = render(editor());
    fireEvent.change(await screen.findByLabelText('editor'), { target: { value: 'My unsaved edit' } });
    fireEvent.change(screen.getByPlaceholderText('标题'), { target: { value: 'My title' } });
    state.page = { ...state.page, contentMd: 'Other author update', title: 'Other title', versionNo: 3 };
    view.rerender(editor());
    expect(screen.getByLabelText('editor')).toHaveValue('My unsaved edit');
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await screen.findByRole('button', { name: 'Reload fixture' });
    expect(state.save).toHaveBeenLastCalledWith(expect.objectContaining({ body: expect.objectContaining({ contentMd: 'My unsaved edit', baseVersionNo: 2, title: 'My title' }) }));
    state.refetch.mockResolvedValue({ data: { ...state.page, contentMd: 'Explicit latest', title: 'Latest title', versionNo: 4 } });
    fireEvent.click(screen.getByRole('button', { name: 'Reload fixture' }));
    await waitFor(() => expect(screen.getByLabelText('editor')).toHaveValue('Explicit latest'));
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(state.save).toHaveBeenLastCalledWith(expect.objectContaining({ body: expect.objectContaining({ contentMd: 'Explicit latest', baseVersionNo: 4, title: 'Latest title' }) })));
  });
  it('publishes a recovered draft against its saved base rather than the latest page base', async () => {
    state.save.mockRejectedValue({ response: { status: 409, data: { currentVersionNo: 2 } } });
    render(editor());
    fireEvent.click(await screen.findByRole('button', { name: 'Recover fixture' }));
    expect(await screen.findByLabelText('source')).toHaveValue('Recovered edits');
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await screen.findByRole('button', { name: 'Reload fixture' });
    expect(state.save).toHaveBeenCalledWith(expect.objectContaining({ body: expect.objectContaining({ contentMd: 'Recovered edits', baseVersionNo: 1 }) }));
  });
});
