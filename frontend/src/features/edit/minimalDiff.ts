import remarkDirective from 'remark-directive';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import remarkParse from 'remark-parse';
import { unified } from 'unified';
import type { Node, Parent, Position } from 'unist';

// Minimal-diff save (design 10 S4.1): the editor (remark serialization) reformats the
// whole document (table alignment, escapes, blank lines). Saving that verbatim would
// make every git diff cover the full file. This keeps original bytes for all blocks
// the user did not touch and only takes the editor output for changed blocks.
//
// Both inputs are BODIES (no frontmatter); the caller re-attaches frontmatter.
export function minimalDiff(originalBody: string, editedBody: string): string {
  const aBlocks = topBlocks(originalBody);
  const bBlocks = topBlocks(editedBody);
  const ops = lcsOpsBlocks(aBlocks, bBlocks);

  const parts: string[] = [];
  let lastEnd = 0;
  const pushGap = (from: number, to: number, fallback: string) => {
    if (to <= from) {
      parts.push(fallback);
      return;
    }
    parts.push(originalBody.slice(from, to));
  };

  for (const op of ops) {
    if (op.kind === 'equal') {
      const a = aBlocks[op.a];
      pushGap(lastEnd, startOf(a), '\n\n');
      parts.push(originalBody.slice(startOf(a), endOf(a)));
      lastEnd = endOf(a);
    } else if (op.kind === 'insert') {
      const b = bBlocks[op.b];
      pushGap(lastEnd, lastEnd, '\n\n');
      parts.push(editedBody.slice(startOf(b), endOf(b)));
      // lastEnd stays: inserted content has no original span
    } else {
      // delete: consume original bytes, emit nothing (keeps later gaps whitespace-only)
      lastEnd = endOf(aBlocks[op.a]);
    }
  }
  // trailing: original tail after the last kept block (keeps/omits final newline as-is)
  const keptEnds = ops.filter(op => op.kind === 'equal');
  if (keptEnds.length === 0) {
    // everything replaced: return the edited body trimmed like the original tail policy
    return editedBody;
  }
  pushGap(lastEnd, originalBody.length, '');
  return parts.join('').replace(/^\n+/, originalBody.startsWith('\n') ? '\n' : '');
}

interface Block extends Parent {
  position: Position;
}

function topBlocks(text: string): Block[] {
  const tree = unified().use(remarkParse).use(remarkGfm).use(remarkDirective).use(remarkMath).parse(text) as Parent;
  return (tree.children as Node[]).filter((n): n is Block => {
    const pos = (n as { position?: Position }).position;
    return pos?.start?.offset !== undefined && pos?.end?.offset !== undefined;
  });
}

function startOf(b: Block): number {
  return b.position.start.offset ?? 0;
}

function endOf(b: Block): number {
  return b.position.end.offset ?? 0;
}

// Stable identity for alignment: structure + normalized text, no positions.
function fingerprint(node: Node): string {
  return hash(JSON.stringify(normalize(node)));
}

function normalize(node: Node): unknown {
  if (node.type === 'text') {
    return { type: 'text', value: (node as unknown as { value: string }).value.trim().replace(/\s+/g, ' ') };
  }
  const out: Record<string, unknown> = { type: node.type };
  for (const [key, value] of Object.entries(node)) {
    if (key === 'position' || key === 'data') continue;
    if (key === 'children' && Array.isArray(value)) {
      out.children = (value as Node[]).map(normalize);
    } else if (key === 'value' && typeof value === 'string') {
      // code blocks keep exact bytes (language + content); other literals normalize whitespace
      out.value = node.type === 'code' || node.type === 'inlineCode' ? value : value.trim().replace(/\s+/g, ' ');
    } else if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value === null) {
      out[key] = value;
    } else if (Array.isArray(value)) {
      out[key] = value.map(item => (typeof item === 'object' && item !== null ? normalize(item as Node) : item));
    }
  }
  return out;
}

// cyrb53: small deterministic 53-bit hash, enough for block identity here.
function hash(text: string): string {
  let h1 = 0xdeadbeef;
  let h2 = 0x41c6ce57;
  for (let i = 0; i < text.length; i++) {
    const ch = text.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
}

type Op = { kind: 'equal'; a: number } | { kind: 'insert'; b: number } | { kind: 'delete'; a: number };

// Render-equivalence second chance (09 最小差异): some normalizations are forced by the
// ProseMirror document model and render identically — uneven table rows filled with empty
// cells, list marker style. Those keep original bytes.
function blocksEqual(a: Block, b: Block): boolean {
  if (fingerprint(a) === fingerprint(b)) return true;
  if (a.type === 'table' && b.type === 'table') return tableCells(a).join('\u0000') === tableCells(b).join('\u0000');
  if (a.type === 'list' && b.type === 'list') return listItems(a).join('\u0000') === listItems(b).join('\u0000');
  return false;
}

function cellText(cell: Node): string {
  const parts: string[] = [];
  const walk = (node: Node) => {
    if (node.type === 'text') parts.push((node as unknown as { value: string }).value.trim());
    for (const child of (node as Parent).children ?? []) walk(child);
  };
  walk(cell);
  return parts.join(' ');
}

function tableCells(table: Block): string[] {
  const rows = (((table as unknown as { children: Node[] }).children ?? []).filter(c => c.type === 'tableRow') as unknown as {
    children: Node[];
  }[]).map(row => (row.children ?? []).filter(c => c.type === 'tableCell').map(cellText));
  const width = Math.max(0, ...rows.map(r => r.length));
  const out: string[] = [];
  for (const row of rows) {
    for (let i = 0; i < width; i++) out.push(row[i] ?? '');
    out.push('\n');
  }
  return out;
}

function listItems(list: Block): string[] {
  const items = ((list as unknown as { children: Node[] }).children ?? []).filter(c => c.type === 'listItem');
  return items.map(item => cellText(item));
}

// LCS (DP) over block sequences; blocks are few hundred at most.
function lcsOpsBlocks(a: Block[], b: Block[]): Op[] {
  const m = a.length;
  const n = b.length;
  const eq: boolean[][] = Array.from({ length: m }, (_, i) => Array.from({ length: n }, (_, j) => blocksEqual(a[i], b[j])));
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array<number>(n + 1).fill(0));
  for (let i = m - 1; i >= 0; i--) {
    for (let j = n - 1; j >= 0; j--) {
      dp[i][j] = eq[i][j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }
  const ops: Op[] = [];
  let i = 0;
  let j = 0;
  while (i < m && j < n) {
    if (eq[i][j]) {
      ops.push({ kind: 'equal', a: i });
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      ops.push({ kind: 'delete', a: i });
      i++;
    } else {
      ops.push({ kind: 'insert', b: j });
      j++;
    }
  }
  while (i < m) {
    ops.push({ kind: 'delete', a: i });
    i++;
  }
  while (j < n) {
    ops.push({ kind: 'insert', b: j });
    j++;
  }
  return ops;
}
