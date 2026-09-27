import { EditorView, basicSetup } from 'codemirror';
import { markdown } from '@codemirror/lang-markdown';
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';

export interface SourceEditorHandle {
  getMarkdown: () => string;
  insertText: (text: string) => void;
}

// Source mode (design 10 S4.2): CodeMirror 6 markdown, edits the raw text directly.
// Saves bypass minimalDiff (stored verbatim). Switching back to WYSIWYG rebuilds
// the Milkdown editor from the source text.
export const SourceEditor = forwardRef<SourceEditorHandle, { value: string; onChange: (markdown: string) => void }>(
  function SourceEditor({ value, onChange }, ref) {
    const rootRef = useRef<HTMLDivElement>(null);
    const viewRef = useRef<EditorView | null>(null);
    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;
    const valueRef = useRef(value);
    valueRef.current = value;

    useEffect(() => {
      const root = rootRef.current;
      if (root === null) return;
      const view = new EditorView({
        doc: valueRef.current,
        extensions: [
          basicSetup,
          markdown(),
          EditorView.lineWrapping,
          EditorView.updateListener.of(update => {
            if (update.docChanged) {
              onChangeRef.current(update.state.doc.toString());
            }
          }),
        ],
        parent: root,
      });
      viewRef.current = view;
      return () => {
        view.destroy();
        viewRef.current = null;
      };
      // mount-once by design, like MarkdownEditor
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useImperativeHandle(ref, () => ({
      getMarkdown: () => viewRef.current?.state.doc.toString() ?? valueRef.current,
      insertText: text => {
        const view = viewRef.current;
        if (view === null) return;
        view.dispatch(view.state.replaceSelection(text));
        view.focus();
      },
    }));

    return <div ref={rootRef} className="source-editor" />;
  },
);
