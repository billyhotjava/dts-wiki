import { Card, Col, Row, Typography } from 'antd';
import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useSpaces } from '../../api/hooks';
import { useAuth } from '../../auth/AuthProvider';
import { AsyncState } from '../../components/AsyncState';

export function HomePage() {
  const { t } = useTranslation();
  const { account } = useAuth();
  const { data, isLoading, isError, refetch } = useSpaces();
  return (
    <>
      <Typography.Title level={3}>{t('home.welcome')}</Typography.Title>
      <Typography.Paragraph>
        {t('home.hello', { name: account?.login ?? '' })} — {t('home.spacesHint')}
      </Typography.Paragraph>
      <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={(data?.length ?? 0) === 0} onRetry={() => void refetch()}>
        <Row gutter={16}>
          {(data ?? []).map(s => (
            <Col key={s.slug} span={8}>
              <Link to={`/s/${s.slug}`}>
                <Card title={s.name} hoverable>
                  {s.description} · {s.pageCount} 页
                </Card>
              </Link>
            </Col>
          ))}
        </Row>
      </AsyncState>
    </>
  );
}
