export interface DiffLine { kind: 'same' | 'add' | 'remove'; text: string }

/** Bound comparison work; large versions still produce a correct replacement diff. */
export function lineDiff(before: string, after: string): DiffLine[] {
  const left = before.split(/\r?\n/), right = after.split(/\r?\n/);
  if (left.length * right.length > 500_000) {
    let start = 0, end = 0;
    while (start < left.length && start < right.length && left[start] === right[start]) start++;
    while (end < left.length - start && end < right.length - start && left[left.length - end - 1] === right[right.length - end - 1]) end++;
    return [...left.slice(0, start).map(text => ({ kind: 'same' as const, text })),
      ...left.slice(start, left.length - end).map(text => ({ kind: 'remove' as const, text })),
      ...right.slice(start, right.length - end).map(text => ({ kind: 'add' as const, text })),
      ...left.slice(left.length - end).map(text => ({ kind: 'same' as const, text }))];
  }
  const width = right.length + 1;
  const table = new Uint32Array((left.length + 1) * width);
  for (let i = left.length - 1; i >= 0; i--) for (let j = right.length - 1; j >= 0; j--) {
    table[i * width + j] = left[i] === right[j] ? 1 + table[(i + 1) * width + j + 1] : Math.max(table[(i + 1) * width + j], table[i * width + j + 1]);
  }
  const result: DiffLine[] = [];
  let i = 0, j = 0;
  while (i < left.length || j < right.length) {
    if (i < left.length && j < right.length && left[i] === right[j]) { result.push({ kind: 'same', text: left[i++] }); j++; }
    else if (i < left.length && (j === right.length || table[(i + 1) * width + j] >= table[i * width + j + 1])) result.push({ kind: 'remove', text: left[i++] });
    else result.push({ kind: 'add', text: right[j++] });
  }
  return result;
}
