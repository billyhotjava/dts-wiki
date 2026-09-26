// Frontmatter split (rule shared with the backend, design 10 S3.1):
// file starts with `---\n` (or `---\r\n`); frontmatter ends at the next line
// that is exactly `---`. No match means no frontmatter.
export function splitFrontmatter(md: string): { front: string; body: string } {
  if (!md.startsWith('---\n') && !md.startsWith('---\r\n')) {
    return { front: '', body: md };
  }
  const lines = md.split('\n');
  for (let i = 1; i < lines.length; i++) {
    if (lines[i].replace(/\r$/, '') === '---') {
      const front = lines.slice(0, i + 1).join('\n') + '\n';
      return { front, body: md.slice(front.length) };
    }
  }
  return { front: '', body: md };
}
