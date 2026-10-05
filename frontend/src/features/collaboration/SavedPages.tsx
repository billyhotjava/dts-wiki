import { Card, Tabs, Button, Alert, Pagination } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';

function SavedList({ kind }: { kind: 'favorites' | 'recent-pages' }) {
  const { t } = useTranslation(); const [page, setPage] = useState(0);
  const data = useQuery({ queryKey: ['saved-pages', kind, page], queryFn: async () => (await api.get<{ items: { pageId: number; title: string; spaceSlug: string; url: string; savedAt: string }[]; total: number }>(`/api/wiki/me/${kind}`, { params: { page: String(page), size: '20' } })).data });
  if (data.isError) return <Alert type="error" title={t('collaboration.failed')} action={<Button onClick={() => void data.refetch()}>{t('collaboration.retry')}</Button>} />;
  return <>{data.isLoading && <p>{t('collaboration.loading')}</p>}
    {data.data?.items.length === 0 && <p>{t('collaboration.empty')}</p>}
    <ul style={{ padding: 0, listStyle: 'none' }}>{(data.data?.items ?? []).map(item => <li key={item.pageId} style={{ padding: 12, borderBottom: '1px solid #eee' }}><Link to={item.url}>{item.title}</Link><p>{item.spaceSlug} · {new Date(item.savedAt).toLocaleString()}</p></li>)}</ul>
    <Pagination current={page + 1} pageSize={20} total={data.data?.total ?? 0} showSizeChanger={false} onChange={number => setPage(number - 1)} /></>;
}
export function SavedPages() {
  const { t } = useTranslation();
  return <Card title={t('collaboration.myPages')}><Tabs items={[
    { key: 'favorites', label: t('collaboration.favorites'), children: <SavedList kind="favorites" /> },
    { key: 'recent', label: t('collaboration.recent'), children: <SavedList kind="recent-pages" /> },
  ]} /></Card>;
}
