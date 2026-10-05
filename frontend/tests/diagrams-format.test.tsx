import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createRef } from 'react';
import { MemoryRouter } from 'react-router';
import { ArchifyFrame, diagramReference } from '../src/components/ArchifyFrame';
import { MarkdownView } from '../src/components/MarkdownView';
import { MarkdownEditor, type MarkdownEditorHandle } from '../src/features/edit/MarkdownEditor';
import { formatMarkdown } from '../src/utils/markdownFormat';
import '../src/i18n';

const reference = { src: 'diagrams/flow.archify.json', height: 560 };
const fence = '```archify src="./diagrams/flow.archify.json" height="560"\n![Flow](diagrams/flow.png)\n```\n';
afterEach(async () => { await act(async () => cleanup()); vi.unstubAllGlobals(); });

describe('Diagram bundles and formatting', () => {
  it('rejects external/traversal iframe references and bounds height', () => {
    expect(diagramReference('archify src="https://example.test/diagram.archify.json"')).toBeNull();
    expect(diagramReference('archify src="./diagrams/../private.archify.json"')).toBeNull();
    expect(diagramReference('archify src="./diagrams/flow.archify.json" height="99999"')).toEqual({ ...reference, height: 1200 });
  });

  it('renders the inline bundle with an opaque script sandbox', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { headers: { 'Content-Type': 'text/html' } })));
    const { container } = render(<MemoryRouter><MarkdownView content={'Before\n\n' + fence + '\nAfter\n'} pageId={42} spaceSlug="notes" /></MemoryRouter>);
    const iframe = await screen.findByTitle('交互图');
    expect(iframe).toHaveAttribute('sandbox', 'allow-scripts');
    expect(iframe).toHaveAttribute('src', '/api/wiki/pages/42/raw/diagrams/flow.html');
    expect(container.textContent).toContain('Before'); expect(container.textContent).toContain('After');
    expect(fetch).toHaveBeenCalledWith('/api/wiki/pages/42/raw/diagrams/flow.html', expect.objectContaining({ method: 'HEAD' }));
  });

  it('falls back to PNG and then a missing-file message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 404 })));
    render(<ArchifyFrame reference={reference} pageId={42} />);
    const image = await screen.findByAltText('交互图的静态预览');
    expect(image).toHaveAttribute('src', '/api/wiki/pages/42/raw/diagrams/flow.png');
    expect(screen.queryByTitle('交互图')).toBeNull();
    fireEvent.error(image);
    expect(screen.getByText('图文件暂不可用，请联系页面作者重新生成图文件。')).toBeVisible();
  });

  it('keeps metadata, callouts, diagram attributes and dialect formatting idempotent', () => {
    const front = '---\ntype: page\ntitle: Formatting fixture\n---\n';
    const formatted = formatMarkdown(front + '* item\n\n> [!NOTE]\n> Notice\n\n' + fence + '\n$E=mc^2$\n');
    expect(formatted).toContain(front); expect(formatted).toContain('- item'); expect(formatted).toContain('> [!NOTE]');
    expect(formatted).toContain(fence); expect(formatted).toContain('$E=mc^2$');
    expect(formatMarkdown(formatted)).toBe(formatted);
  });

  it('preserves fence attributes in a real editor and exposes a read-only card', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { headers: { 'Content-Type': 'text/html' } })));
    const ref = createRef<MarkdownEditorHandle>();
    const { container } = render(<MarkdownEditor ref={ref} value={fence} pageId={42} onChange={() => {}} onUploadImage={async () => ''} />);
    await waitFor(() => expect(container.querySelector('.wiki-archify-editor')).not.toBeNull(), { timeout: 5000 });
    expect(container.querySelector('.wiki-archify-editor')).toHaveAttribute('contenteditable', 'false');
    expect(ref.current?.getMarkdown()).toContain('archify src="./diagrams/flow.archify.json" height="560"');
    expect(ref.current?.getMarkdown()).toContain('![Flow](diagrams/flow.png)');
  });
});
