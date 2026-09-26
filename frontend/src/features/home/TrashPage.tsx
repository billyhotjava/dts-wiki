import { Button, List, Typography, message } from 'antd';
import { useParams } from 'react-router';
import { useRestorePage, useTrash } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

// Trash page (design 05 S2 /s/:slug/trash): restore soft-deleted pages.
export function TrashPage() {
  const { slug = '' } = useParams();
  const { data, isLoading, isError, refetch } = useTrash(slug);
  const restore = useRestorePage();

  return (
    <>
      <Typography.Title level={4}>回收站</Typography.Title>
      <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={(data?.length ?? 0) === 0} onRetry={() => void refetch()}>
        <List
          dataSource={data ?? []}
          renderItem={id => (
            <List.Item actions={[<Button onClick={() => restore.mutate({ id, slug }, { onError: () => message.error('恢复失败') })}>恢复</Button>]}>
              页面 #{id}
            </List.Item>
          )}
        />
      </AsyncState>
    </>
  );
}
