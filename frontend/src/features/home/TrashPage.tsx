import { Button, Pagination, Typography, message } from 'antd';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { useRestorePage, useTrash } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

// Trash page (design 05 S2 /s/:slug/trash): restore soft-deleted pages.
export function TrashPage() {
  const { slug = '' } = useParams();
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useTrash(slug, page);
  const restore = useRestorePage();

  return (
    <>
      <Typography.Title level={4}>{t('trash.title')}</Typography.Title>
      <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={data?.total === 0} onRetry={() => void refetch()}>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {(data?.items ?? []).map(item => <li key={item.id} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0', borderBottom: '1px solid #eee' }}>
            <div style={{ flex: 1 }}><Typography.Text>{item.title}</Typography.Text><br />
              <Typography.Text type="secondary">{new Date(item.deletedAt).toLocaleString()}</Typography.Text>
            </div>
            <Button disabled={!item.restorable || restore.isPending} onClick={() => restore.mutate({ id: item.id, slug }, { onError: () => void message.error(t('trash.restoreFailed')) })}>{t('trash.restore')}</Button>
          </li>)}
        </ul>
        {(data?.total ?? 0) > 50 && <Pagination current={page + 1} pageSize={50} total={data?.total} showSizeChanger={false} onChange={value => setPage(value - 1)} />}
      </AsyncState>
    </>
  );
}
