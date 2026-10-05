import { Alert, Button, Space, Spin } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface DiagramReference { src: string; height: number }

/** Accept only local diagram bundles, never an arbitrary iframe destination. */
export function diagramReference(info: string): DiagramReference | null {
  const src = /(?:^|\s)src="([^"]+)"/.exec(info)?.[1];
  if (!src || !/^(?:\.\/)?diagrams\/[a-zA-Z0-9][a-zA-Z0-9._-]*\.archify\.json$/.test(src)) return null;
  const rawHeight = /(?:^|\s)height="(\d+)"/.exec(info)?.[1];
  return { src: src.replace(/^\.\//, ''), height: Math.min(1200, Math.max(320, rawHeight ? Number(rawHeight) : 520)) };
}

export function ArchifyFrame({ pageId, reference }: { pageId: number; reference: DiagramReference }) {
  const { t } = useTranslation();
  const host = useRef<HTMLDivElement>(null);
  const [state, setState] = useState<'loading' | 'html' | 'png' | 'missing'>('loading');
  const base = reference.src.replace(/\.archify\.json$/, '');
  const raw = `/api/wiki/pages/${pageId}/raw/`;
  const html = raw + base + '.html', png = raw + base + '.png', source = raw + reference.src;
  useEffect(() => {
    const abort = new AbortController();
    setState('loading');
    void fetch(html, { method: 'HEAD', credentials: 'same-origin', signal: abort.signal })
      .then(response => { if (!abort.signal.aborted) setState(response.ok && response.headers.get('Content-Type')?.includes('text/html') ? 'html' : 'png'); })
      .catch(() => { if (!abort.signal.aborted) setState('png'); });
    return () => abort.abort();
  }, [html]);
  return <div ref={host} className="wiki-diagram" style={{ margin: '16px 0' }}>
    <Space wrap style={{ marginBottom: 8 }}>
      <Button onClick={() => { void host.current?.requestFullscreen?.().catch(() => undefined); }}>{t('diagram.fullscreen')}</Button>
      <Button href={html} target="_blank" rel="noopener noreferrer">{t('diagram.open')}</Button>
      <Button href={source} target="_blank" rel="noopener noreferrer">{t('diagram.source')}</Button>
      <Button href={png} download>{t('diagram.download')}</Button>
    </Space>
    {state === 'loading' && <Spin description={t('diagram.loading')} />}
    {state === 'html' && <iframe title={t('diagram.title')} src={html} sandbox="allow-scripts" loading="lazy"
      style={{ width: '100%', height: reference.height, border: '1px solid #d9d9d9', borderRadius: 6 }} onError={() => setState('png')} />}
    {state === 'png' && <img src={png} alt={t('diagram.fallback')} style={{ width: '100%' }} onError={() => setState('missing')} />}
    {state === 'missing' && <Alert type="warning" title={t('diagram.missing')} />}
  </div>;
}
