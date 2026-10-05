import { Card, Space, Typography } from 'antd';
import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useActivity } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

export function ActivityPanel({ space, author }: { space?: string; author?: string }) {
  const { t } = useTranslation();
  const params: Record<string, string> = { size: '20' };
  if (space) params.space = space;
  if (author) params.author = author;
  const { data, isLoading, isError, refetch } = useActivity(params);
  return <Card title={t('activity.title')} style={{ marginTop: 24 }}>
    <AsyncState loading={isLoading} error={isError ? new Error('activity failed') : null} empty={data?.length === 0} onRetry={() => void refetch()}>
      <Space orientation="vertical" style={{ width: '100%' }}>{data?.map(item => <div key={item.id}>
        {item.url ? <Link to={item.url}>{item.title}</Link> : <Typography.Text>{item.title}</Typography.Text>}
        <Typography.Text type="secondary"> · {t(`activity.${item.type}`, item.type)} · {item.actor} · {new Date(item.createdAt).toLocaleString()}</Typography.Text>
      </div>)}</Space>
    </AsyncState>
  </Card>;
}
