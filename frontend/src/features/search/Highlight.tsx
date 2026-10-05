/** Render escaped text nodes; search terms never become HTML. */
export function Highlight({ text, term }: { text: string; term: string }) {
  if (!term.trim()) return <>{text}</>;
  const escaped = term.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return <>{text.split(new RegExp(`(${escaped})`, 'gi')).map((part, index) =>
    part.toLocaleLowerCase() === term.trim().toLocaleLowerCase() ? <mark key={index}>{part}</mark> : part)}</>;
}
