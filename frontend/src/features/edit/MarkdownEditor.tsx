import { Crepe } from '@milkdown/crepe';
import { imageBlock } from '@milkdown/crepe/feature/image-block';
import '@milkdown/crepe/theme/frame.css';
import { editorViewCtx, remarkStringifyOptionsCtx } from '@milkdown/kit/core';
import { codeBlockSchema } from '@milkdown/kit/preset/commonmark';
import type { NodeViewConstructor } from '@milkdown/kit/prose/view';
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import i18next from 'i18next';
import { splitFrontmatter } from '../../utils/frontmatter';
import { markdownFormatOptions, restoreCallouts } from '../../utils/markdownFormat';
import { ArchifyFrame, diagramReference } from '../../components/ArchifyFrame';
export { restoreCallouts } from '../../utils/markdownFormat';

export interface MarkdownEditorHandle {
  /** Full markdown including the (preserved) frontmatter. */
  getMarkdown: () => string;
  /** Insert text at the current cursor (used by @mention picker). */
  insertText: (text: string) => void;
}

interface Props {
  /** Initial full markdown (may carry YAML frontmatter, kept out of the edit area). */
  value: string;
  /** Called only on user edits (never on mount when untouched). */
  onChange: (markdown: string) => void;
  onUploadImage: (file: File) => Promise<string>;
  readOnly?: boolean;
  pageId?: number;
}


// GitHub-alerts callouts (E2): Milkdown escapes `[!NOTE]` to `\[!NOTE]` inside
// blockquotes, which breaks alert detection in GitHub and our renderer.
// Unescape only at blockquote starts (same lines the parser will treat as alerts).
// A full callout NodeView (antd Alert styling) + `/` menu insertion is follow-up
// polish; content integrity is what matters here and is covered by tests.

// Markdown-native WYSIWYG wrapper (design 05 S4, spike assets/editor-spike.md).
// The editor only ever sees the body; frontmatter is re-attached on export so
// git pages keep their metadata byte-identical when untouched.
export const MarkdownEditor = forwardRef<MarkdownEditorHandle, Props>(function MarkdownEditor(
  { value, onChange, onUploadImage, readOnly = false, pageId },
  ref,
) {
  const rootRef = useRef<HTMLDivElement>(null);
  const crepeRef = useRef<Crepe | null>(null);
  const frontRef = useRef('');
  const lastEmittedRef = useRef('');
  const onChangeRef = useRef(onChange);
  const uploadRef = useRef(onUploadImage);
  onChangeRef.current = onChange;
  uploadRef.current = onUploadImage;

  useEffect(() => {
    const root = rootRef.current;
    if (root === null) return;
    const { front, body } = splitFrontmatter(value);
    frontRef.current = front;
    lastEmittedRef.current = body;
    const crepe = new Crepe({ root, defaultValue: body });
    crepe.editor.config(ctx => ctx.update(remarkStringifyOptionsCtx, options => ({ ...options, ...markdownFormatOptions })));
    // Preserve fence metadata (src/height); the default code schema keeps only lang.
    crepe.editor.use(codeBlockSchema.extendSchema(original => context => {
      const schema = original(context);
      return { ...schema, attrs: { ...schema.attrs, meta: { default: '' } },
        parseMarkdown: { ...schema.parseMarkdown, runner: (state, node, type) => {
          state.openNode(type, { language: String(node.lang ?? ''), meta: String(node.meta ?? '') });
          if (node.value) state.addText(String(node.value));
          state.closeNode();
        } },
        toMarkdown: { ...schema.toMarkdown, runner: (state, node) => {
          if (String(node.attrs.language).toLowerCase() === 'latex') {
            state.addNode('math', undefined, node.textContent);
            return;
          }
          state.addNode('code', undefined, node.textContent, { lang: node.attrs.language, meta: node.attrs.meta || null });
        } },
      };
    }));
    crepeRef.current = crepe;
    crepe.addFeature(imageBlock, {
      onUpload: (file: File) => uploadRef.current(file),
      inlineOnUpload: (file: File) => uploadRef.current(file),
      blockOnUpload: (file: File) => uploadRef.current(file),
    });
    if (readOnly) {
      crepe.setReadonly(true);
    }
    crepe.on(listener => {
      listener.markdownUpdated((_ctx, markdown) => {
        const restored = restoreCallouts(markdown);
        if (restored !== lastEmittedRef.current) {
          lastEmittedRef.current = restored;
          onChangeRef.current(frontRef.current + restored);
        }
      });
    });
    let active = true;
    void crepe.create().then(() => {
      if (!active) return;
      crepe.editor.action(ctx => {
        const view = ctx.get(editorViewCtx);
        let original: NodeViewConstructor | undefined;
        view.someProp('nodeViews', views => { if (views.code_block) { original = views.code_block; return true; } return false; });
        view.setProps({ nodeViews: { ...view.props.nodeViews, code_block: (node, editor, position, decorations, inner) => {
          if (node.attrs.language !== 'archify') {
            if (original) return original(node, editor, position, decorations, inner);
            const pre = document.createElement('pre'), code = document.createElement('code');
            pre.appendChild(code); return { dom: pre, contentDOM: code };
          }
          const dom = document.createElement('section');
          dom.setAttribute('contenteditable', 'false'); dom.className = 'wiki-archify-editor';
          const preview = createRoot(dom);
          const reference = diagramReference(`archify ${node.attrs.meta}`);
          preview.render(<><p>{i18next.t('diagram.readOnly')}</p>{reference && pageId
            ? <ArchifyFrame pageId={pageId} reference={reference} /> : <pre>{node.textContent}</pre>}</>);
          return { dom, ignoreMutation: () => true, stopEvent: () => true,
            destroy: () => { queueMicrotask(() => preview.unmount()); } };
        } } });
      });
    });
    return () => {
      active = false;
      void crepe.destroy();
      crepeRef.current = null;
    };
    // mount-once by design: parent remounts (via key) when switching pages
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useImperativeHandle(ref, () => ({
    getMarkdown: () => frontRef.current + restoreCallouts(crepeRef.current?.getMarkdown() ?? lastEmittedRef.current),
    insertText: text => {
      const crepe = crepeRef.current;
      if (crepe === null) return;
      crepe.editor.action(ctx => {
        const view = ctx.get(editorViewCtx);
        const { from, to } = view.state.selection;
        view.dispatch(view.state.tr.insertText(text, from, to));
        view.focus();
      });
    },
  }));

  return <div ref={rootRef} />;
});
