import { Crepe } from '@milkdown/crepe';
import { imageBlock } from '@milkdown/crepe/feature/image-block';
import '@milkdown/crepe/theme/frame.css';
import { editorViewCtx } from '@milkdown/kit/core';
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { splitFrontmatter } from '../../utils/frontmatter';

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
}


// GitHub-alerts callouts (E2): Milkdown escapes `[!NOTE]` to `\[!NOTE]` inside
// blockquotes, which breaks alert detection in GitHub and our renderer.
// Unescape only at blockquote starts (same lines the parser will treat as alerts).
// A full callout NodeView (antd Alert styling) + `/` menu insertion is follow-up
// polish; content integrity is what matters here and is covered by tests.
export function restoreCallouts(md: string): string {
  return md.replace(/^((?:> ?)+)\\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]/gm, '$1[!$2]');
}

// Markdown-native WYSIWYG wrapper (design 05 S4, spike assets/editor-spike.md).
// The editor only ever sees the body; frontmatter is re-attached on export so
// git pages keep their metadata byte-identical when untouched.
export const MarkdownEditor = forwardRef<MarkdownEditorHandle, Props>(function MarkdownEditor(
  { value, onChange, onUploadImage, readOnly = false },
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
    void crepe.create();
    return () => {
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
