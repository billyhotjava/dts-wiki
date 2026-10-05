import { Button, Card, Col, Input, Pagination, Row, Select, Space, Tag, Typography } from 'antd';
import { Link, useParams, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useContentQuery } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

const statuses = ['DRAFT', 'READY', 'IN_PROGRESS', 'BLOCKED', 'DONE'];
const pageSize = 200;

export function SprintBoardPage() {
  const { slug = '' } = useParams();
  const [search, setSearch] = useSearchParams();
  const { t } = useTranslation();
  const page = Math.max(0, Number(search.get('page') ?? '0') || 0);
  const filters: Record<string, string> = { space: slug, type: 'task', page: String(page), size: String(pageSize) };
  for (const key of ['sprint', 'feature', 'priority', 'owner', 'status']) {
    const value = search.get(key);
    if (value) filters[key] = value;
  }
  const { data, isLoading, isError, refetch } = useContentQuery(filters);
  function change(key: string, value: string | undefined) {
    const next = new URLSearchParams(search);
    next.delete('page');
    if (value) next.set(key, value); else next.delete(key);
    setSearch(next, { replace: true });
  }

  return <Space orientation="vertical" style={{ width: '100%' }} size="large">
    <Typography.Title level={3}>{t('board.title')}</Typography.Title>
    <Space wrap>
      <Input aria-label={t('board.sprint')} placeholder={t('board.sprint')} value={search.get('sprint') ?? ''} onChange={e => change('sprint', e.target.value)} style={{ width: 160 }} />
      <Input aria-label={t('board.feature')} placeholder={t('board.feature')} value={search.get('feature') ?? ''} onChange={e => change('feature', e.target.value)} style={{ width: 140 }} />
      <Input aria-label={t('board.owner')} placeholder={t('board.owner')} value={search.get('owner') ?? ''} onChange={e => change('owner', e.target.value)} style={{ width: 140 }} />
      <Select aria-label={t('board.priority')} placeholder={t('board.priority')} allowClear value={search.get('priority') ?? undefined} onChange={v => change('priority', v)} style={{ width: 120 }} options={['P0', 'P1', 'P2'].map(value => ({ value, label: value }))} />
      <Select aria-label={t('board.status')} placeholder={t('board.status')} allowClear value={search.get('status') ?? undefined} onChange={v => change('status', v)} style={{ width: 150 }} options={statuses.map(value => ({ value, label: t(`status.${value}`) }))} />
      <Button onClick={() => setSearch({})}>{t('board.clear')}</Button>
    </Space>
    <Typography.Text type="secondary">{t('board.count', { count: data?.items.length ?? 0, total: data?.total ?? 0 })}</Typography.Text>
    <AsyncState loading={isLoading} error={isError ? new Error('query failed') : null} empty={data?.items.length === 0} onRetry={() => void refetch()}>
      <Row gutter={[12, 12]} style={{ flexWrap: 'nowrap', overflowX: 'auto' }}>
        {statuses.map(status => <Col key={status} flex="1 0 190px">
          <Card title={`${t(`status.${status}`)} (${data?.items.filter(row => row.status === status).length ?? 0})`} size="small" style={{ minHeight: 240, background: '#fafafa' }}>
            <Space orientation="vertical" style={{ width: '100%' }}>
              {data?.items.filter(row => row.status === status).map(row => <Card key={row.pageId} size="small">
                <Link to={row.url}>{row.title}</Link>
                <div><Typography.Text type="secondary">{row.docId}</Typography.Text></div>
                <Space wrap><Tag>{row.priority ?? '—'}</Tag><Typography.Text>{row.owner}</Typography.Text>{!row.valid && <Tag color="warning">{t('board.invalid')}</Tag>}</Space>
              </Card>)}
            </Space>
          </Card>
        </Col>)}
      </Row>
    </AsyncState>
    <Pagination current={page + 1} pageSize={pageSize} total={data?.total ?? 0} showSizeChanger={false} onChange={value => change('page', value > 1 ? String(value - 1) : undefined)} />
  </Space>;
}
