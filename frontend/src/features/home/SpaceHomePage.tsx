import { lazy, Suspense } from 'react';
import { Button, Space, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { useSpace } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

const ActivityPanel = lazy(() => import('../history/ActivityPanel').then(m => ({ default: m.ActivityPanel })));

// Space overview keeps recent updates and the native-page entry accessible.
export function SpaceHomePage() {
  const { slug = '' } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError, refetch } = useSpace(slug);
  const { t } = useTranslation();

  return (
    <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={data === undefined} onRetry={() => void refetch()}>
      {data && <>
        <Typography.Title level={3}>{data.name}</Typography.Title>
        <Typography.Paragraph>{data.description}</Typography.Paragraph>
        <Space wrap>
          {data.rootPageId && <Button onClick={() => navigate(`/s/${slug}/p/${data.rootPageId}`)}>{t('space.browse')}</Button>}
          {data.editable && <Button type="primary" onClick={() => navigate(`/s/${slug}/new${data.rootPageId ? `?parent=${data.rootPageId}` : ''}`)}>{t('space.create')}</Button>}
          <Button onClick={() => navigate(`/s/${slug}/board`)}>{t('board.title')}</Button>
        </Space>
        <Suspense><ActivityPanel space={slug} /></Suspense>
      </>}
    </AsyncState>
  );
}
