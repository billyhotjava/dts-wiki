import { Button, Space, Alert } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';

export function PersonalActions({ pageId }: { pageId: number }) {
  const { t } = useTranslation(); const client = useQueryClient();
  const state = useQuery({ queryKey: ['personal', pageId], queryFn: async () => (await api.get<{ favorite: boolean; watching: boolean }>(`/api/wiki/pages/${pageId}/personal`)).data });
  const update = useMutation({ mutationFn: async (kind: 'favorite' | 'watch') => {
    const enabled = kind === 'favorite' ? state.data?.favorite : state.data?.watching;
    const url = kind === 'favorite' ? `/api/wiki/me/favorites/${pageId}` : `/api/wiki/pages/${pageId}/watch`;
    if (enabled) await api.delete(url); else await api.put(url);
  }, onSuccess: () => { void client.invalidateQueries({ queryKey: ['personal', pageId] }); void client.invalidateQueries({ queryKey: ['saved-pages'] }); } });
  return <Space wrap>
    <Button disabled={!state.data} loading={update.isPending} onClick={() => update.mutate('favorite')}>{t(state.data?.favorite ? 'collaboration.unfavorite' : 'collaboration.favorite')}</Button>
    <Button disabled={!state.data} loading={update.isPending} onClick={() => update.mutate('watch')}>{t(state.data?.watching ? 'collaboration.unwatch' : 'collaboration.watch')}</Button>
    {(state.isError || update.isError) && <Alert type="error" title={t('collaboration.failed')} action={<Button onClick={() => { update.reset(); void state.refetch(); }}>{t('collaboration.retry')}</Button>} />}
  </Space>;
}
