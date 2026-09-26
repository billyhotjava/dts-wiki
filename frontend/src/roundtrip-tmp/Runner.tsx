import { useEffect, useRef, useState } from 'react';
import { MarkdownEditor, type MarkdownEditorHandle } from '../features/edit/MarkdownEditor';

// TEMPORARY (W5 acceptance only; deleted before merge): mounts every sample in the
// real editor and reports idempotence as JSON. Not part of the shipped app.
const SAMPLES = import.meta.glob('./samples/*.md', { query: '?raw', import: 'default', eager: true }) as Record<string, string>;

interface Row {
  file: string;
  bytes: number;
  zeroDiff: boolean;
  idempotent: boolean;
  onChange: number;
  error?: string;
}

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

async function settle(ref: React.RefObject<MarkdownEditorHandle | null>): Promise<string> {
  let last = '';
  let stable = 0;
  for (let i = 0; i < 60; i++) {
    await sleep(250);
    let cur = '';
    try {
      cur = ref.current?.getMarkdown() ?? '';
    } catch {
      cur = '__CRASH__';
      break;
    }
    if (cur === last && cur !== '') {
      stable++;
      if (stable >= 3) return cur;
    } else {
      stable = 0;
    }
    last = cur;
  }
  return last;
}

export function RoundtripRunner() {
  const [rows, setRows] = useState<Row[]>([]);
  const [finished, setFinished] = useState(false);
  const [current, setCurrent] = useState<{ file: string; input: string; pass: 1 | 2; first: string; changes: number } | null>(null);
  const ref = useRef<MarkdownEditorHandle | null>(null);
  const files = useRef(Object.keys(SAMPLES).sort());

  useEffect(() => {
    if (finished || current !== null) return;
    const next = files.current.shift();
    if (next === undefined) {
      setFinished(true);
      return;
    }
    const input = SAMPLES[next] as string;
    setCurrent({ file: next.split('/').pop() ?? next, input, pass: 1, first: '', changes: 0 });
  }, [rows, current, finished]);

  useEffect(() => {
    if (current === null || finished) return;
    let cancelled = false;
    (async () => {
      const out = await settle(ref);
      if (cancelled) return;
      if (current.pass === 1) {
        const changes = 0;
        setCurrent({ ...current, pass: 2, first: out, changes });
      } else {
        const row: Row = {
          file: current.file,
          bytes: current.input.length,
          zeroDiff: current.first === current.input && current.first !== '__CRASH__' && current.first !== '',
          idempotent: out === current.first && out !== '__CRASH__' && out !== '',
          onChange: current.changes,
          error: out === '__CRASH__' ? 'editor crashed' : undefined,
        };
        setCurrent(null);
        setRows(prev => [...prev, row]);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [current]);

  if (finished) {
    return <pre id="rt-result">{JSON.stringify(rows, null, 1)}</pre>;
  }
  return (
    <div>
      <div>
        {current?.file} pass {current?.pass}
      </div>
      {current !== null && (
        <MarkdownEditor
          key={`${current.file}-${current.pass}`}
          ref={ref}
          value={current.pass === 1 ? current.input : current.first}
          onChange={() => setCurrent(c => (c === null ? c : { ...c, changes: c.changes + 1 }))}
          onUploadImage={async () => ''}
        />
      )}
      <pre id="rt-result">RUNNING {rows.length}/20</pre>
    </div>
  );
}
