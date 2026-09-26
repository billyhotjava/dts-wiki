import { Crepe } from '@milkdown/crepe';
import { imageBlock } from '@milkdown/crepe/feature/image-block';
import '@milkdown/crepe/theme/frame.css';
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';

export interface MarkdownEditorHandle {
  /** Full markdown including the (preserved) frontmatter. */
  getMarkdown: () => string;
}

interface Props {
  /** Initial full markdown (may carry YAML frontmatter, kept out of the edit area). */
  value: string;
  /** Called only on user edits (never on mount when untouched). */
  onChange: (markdown: string) => void;
  onUploadImage: (file: File) => Promise<string>;
  readOnly?: boolean;
}

function splitFrontmatter(md: string): { front: string; body: string } {
  const match = md.match(/^---\n.*?\n---\n?/s);
  if (match !== null && md.startsWith('---\n')) {
    return { front: match[0], body: md.slice(match[0].length) };
  }
  return { front: '', body: md };
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
        if (markdown !== lastEmittedRef.current) {
          lastEmittedRef.current = markdown;
          onChangeRef.current(frontRef.current + markdown);
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
    getMarkdown: () => frontRef.current + (crepeRef.current?.getMarkdown() ?? lastEmittedRef.current),
  }));

  return <div ref={rootRef} />;
});
