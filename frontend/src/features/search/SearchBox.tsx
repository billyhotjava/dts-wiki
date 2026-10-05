import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useSearch } from '../../api/hooks';
import { Highlight } from './Highlight';

export function SearchBox() {
  const [input, setInput] = useState('');
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const { t } = useTranslation();
  useEffect(() => { const timer = window.setTimeout(() => setQuery(input.trim()), 300); return () => window.clearTimeout(timer); }, [input]);
  const { data, isLoading, isError } = useSearch({ q: query, size: '8' });
  return <form style={{ position: 'relative', width: 260, lineHeight: 'normal' }} onSubmit={e => { e.preventDefault(); if (input.trim()) { setOpen(false); navigate(`/search?q=${encodeURIComponent(input.trim())}`); } }}>
    <input aria-label={t('search.placeholder')} placeholder={t('search.placeholder')} maxLength={200} value={input}
      onChange={e => { setInput(e.target.value); setOpen(true); }} onFocus={() => setOpen(true)} onBlur={() => setOpen(false)}
      style={{ width: '100%', border: '1px solid #d9d9d9', borderRadius: 6, padding: '7px 10px', font: 'inherit' }} />
    {open && query && <div onMouseDown={e => e.preventDefault()} style={{ position: 'absolute', zIndex: 100, top: '100%', left: 0, width: 380, maxWidth: '90vw', background: '#fff', boxShadow: '0 6px 20px #0002', padding: 12, borderRadius: 6 }}>
      {isLoading ? t('search.loading') : isError ? t('search.failed') : data?.items.length === 0 ? t('search.empty') :
        data?.items.map(hit => <Link key={hit.pageId} to={hit.url} onClick={() => setOpen(false)} style={{ display: 'block', padding: '8px 0' }}>
          <Highlight text={hit.title} term={query} /><div style={{ fontSize: 12, color: '#888' }}>{hit.spaceName}</div>
        </Link>)}
    </div>}
  </form>;
}
