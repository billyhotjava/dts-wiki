import { Alert, Badge, Button, Pagination, Popover, Space } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';

export function NotificationsBell() {
  const { t } = useTranslation(); const navigate = useNavigate(); const client = useQueryClient();
  const [open, setOpen] = useState(false), [page, setPage] = useState(0);
  const count = useQuery({ queryKey: ['notices', 'count'], queryFn: async () => (await api.get<{ count: number }>('/api/wiki/me/notifications/unread-count')).data, retry: false, refetchInterval: 60000 });
  const notices = useQuery({ queryKey: ['notices', page], enabled: open, retry: false, queryFn: async () => (await api.get<{ items: { id: number; title: string; type: string; read: boolean; createdAt: string }[]; total: number }>('/api/wiki/me/notifications', { params: { page: String(page), size: '10' } })).data });
  const mark = useMutation({ mutationFn: async (id: number | null) => {
    if (id === null) await api.post('/api/wiki/me/notifications/read-all');
    else { const result = await api.post<{ url: string }>(`/api/wiki/me/notifications/${id}/open`); navigate(result.data.url); setOpen(false); }
  }, onSuccess: () => void client.invalidateQueries({ queryKey: ['notices'] }) });
  const error = count.isError || notices.isError || mark.isError;
  const content = <div style={{ width: 340 }}>
    {error && <Alert type="warning" title={t('collaboration.identityUnavailable')} action={<Button onClick={() => { mark.reset(); void count.refetch(); void notices.refetch(); }}>{t('collaboration.retry')}</Button>} />}
    <Button disabled={error} onClick={() => mark.mutate(null)}>{t('collaboration.readAll')}</Button>
    {notices.isLoading && <p>{t('collaboration.loading')}</p>}
    {!error && notices.data?.items.length === 0 && <p>{t('collaboration.noNotifications')}</p>}
    <ul style={{ padding: 0, listStyle: 'none' }}>{(error ? [] : notices.data?.items ?? []).map(item => <li key={item.id} style={{ borderBottom: '1px solid #eee' }}><Button type="link" onClick={() => mark.mutate(item.id)} style={{ height: 'auto', whiteSpace: 'normal', textAlign: 'left' }}><Space orientation="vertical" size={0}><span>{!item.read && '● '}{item.title}</span><small>{t(`collaboration.noticeTypes.${item.type}`)} · {new Date(item.createdAt).toLocaleString()}</small></Space></Button></li>)}</ul>
    <Pagination size="small" current={page + 1} pageSize={10} total={notices.data?.total ?? 0} showSizeChanger={false} onChange={value => setPage(value - 1)} />
  </div>;
  return <Popover open={open} onOpenChange={setOpen} trigger="click" content={content} title={t('collaboration.notifications')}>
    <Badge count={count.isError ? '!' : count.data?.count ?? 0}><Button aria-label={t('collaboration.notifications')} icon={<BellOutlined />} /></Badge>
  </Popover>;
}
