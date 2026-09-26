// Bundle budget gate (design 10 S4.5, F2/T12): fails `pnpm build` when the reading
// page exceeds budget. Reads dist/.vite/manifest.json (vite manifest:true).
// Budgets are gzip bytes: home entry (react+antd+app, editor excluded) JS <= 300 KB,
// CSS <= 60 KB.
import { readFileSync } from 'node:fs';
import { gzipSync } from 'node:zlib';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const manifest = JSON.parse(readFileSync(join(ROOT, 'dist/.vite/manifest.json'), 'utf8'));

const BUDGET_JS_GZ = 300 * 1024;
const BUDGET_CSS_GZ = 60 * 1024;

function assetBytes(file) {
  try {
    return readFileSync(join(ROOT, 'dist', file));
  } catch {
    console.error(`missing asset in manifest: ${file}`);
    process.exit(1);
  }
}

// entry = index.html's JS (the home chunk graph, editor is async and excluded)
const entries = Object.values(manifest).filter(e => e.isEntry);
if (entries.length === 0) {
  console.error('no entry found in manifest');
  process.exit(1);
}
const seen = new Set();
let js = 0;
let css = 0;
function add(file) {
  if (seen.has(file)) return;
  seen.add(file);
  const bytes = assetBytes(file);
  if (file.endsWith('.js')) js += gzipSync(bytes).length;
  if (file.endsWith('.css')) css += gzipSync(bytes).length;
}
for (const entry of entries) {
  // editor chunk (and its deps) must not count toward the reading-page budget
  if (entry.file.includes('editor')) continue;
  add(entry.file);
  for (const dep of entry.imports ?? []) {
    const chunk = manifest[dep] ?? Object.values(manifest).find(v => v.file === dep);
    if (chunk && !chunk.file.includes('editor')) {
      add(chunk.file);
      for (const cssFile of chunk.css ?? []) add(cssFile);
    }
  }
  for (const cssFile of entry.css ?? []) add(cssFile);
}

console.log(`home JS gzip: ${(js / 1024).toFixed(1)} KB (budget 300 KB)`);
console.log(`home CSS gzip: ${(css / 1024).toFixed(1)} KB (budget 60 KB)`);
let failed = false;
if (js > BUDGET_JS_GZ) {
  console.error('BUDGET EXCEEDED: home JS');
  failed = true;
}
if (css > BUDGET_CSS_GZ) {
  console.error('BUDGET EXCEEDED: home CSS');
  failed = true;
}
process.exit(failed ? 1 : 0);
