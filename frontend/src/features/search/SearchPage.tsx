import { Card, Input, Pagination, Select, Space, Tag, Typography } from 'antd';
import { Link, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useSearch } from '../../api/hooks';
import { useAuth } from '../../auth/AuthProvider';
import { AsyncState } from '../../components/AsyncState';
import { Highlight } from './Highlight';

export function SearchPage() {
  const [params, setParams] = useSearchParams();
  const { spaces } = useAuth();
  const { t } = useTranslation();
  const query = params.get('q') ?? '';
  const page = Math.max(0, Number(params.get('page') ?? '0') || 0);
  const filters: Record<string, string> = { q: query, page: String(page), size: '20' };
  for (const key of ['space', 'type', 'status', 'owner', 'tag', 'since', 'until']) {
    const value = params.get(key);
    if (value) filters[key] = value;
  }
  const { data, isLoading, isError, refetch } = useSearch(filters);
  function change(key: string, value?: string) {
    const next = new URLSearchParams(params); next.delete('page');
    if (value) next.set(key, value); else next.delete(key);
    setParams(next, { replace: true });
  }
  const dateValue = (key: string) => params.get(key)?.slice(0, 10) ?? '';
  const changeDate = (key: string, value: string) => change(key, value ? new Date(`${value}T${key === 'until' ? '23:59:59.999' : '00:00:00'}`).toISOString() : undefined);
  return <Space orientation="vertical" size="large" style={{ width: '100%', maxWidth: 1000 }}>
    <Typography.Title level={3}>{t('search.title')}</Typography.Title>
    <Input.Search aria-label={t('search.placeholder')} placeholder={t('search.placeholder')} defaultValue={query} maxLength={200} onSearch={v => change('q', v.trim())} enterButton />
    <Space wrap>
      <Select aria-label={t('search.space')} placeholder={t('search.space')} allowClear value={params.get('space') ?? undefined} onChange={v => change('space', v)} style={{ width: 180 }} options={spaces.map(s => ({ value: s.slug, label: s.name }))} />
      <Select aria-label={t('search.type')} placeholder={t('search.type')} allowClear value={params.get('type') ?? undefined} onChange={v => change('type', v)} style={{ width: 140 }} options={['page','task','feature','sprint','adr','evidence'].map(value => ({ value, label: t(`docType.${value}`) }))} />
      <Select aria-label={t('board.status')} placeholder={t('board.status')} allowClear value={params.get('status') ?? undefined} onChange={v => change('status', v)} style={{ width: 140 }} options={['DRAFT','READY','IN_PROGRESS','BLOCKED','DONE'].map(value => ({ value, label: t(`status.${value}`) }))} />
      <Input aria-label={t('board.owner')} placeholder={t('board.owner')} value={params.get('owner') ?? ''} onChange={e => change('owner', e.target.value)} style={{ width: 140 }} />
      <Input aria-label={t('search.tag')} placeholder={t('search.tag')} value={params.get('tag') ?? ''} onChange={e => change('tag', e.target.value)} style={{ width: 140 }} />
      <label>{t('search.since')} <input type="date" value={dateValue('since')} onChange={e => changeDate('since', e.target.value)} /></label>
      <label>{t('search.until')} <input type="date" value={dateValue('until')} onChange={e => changeDate('until', e.target.value)} /></label>
    </Space>
    {query.trim() && <>
      <Typography.Text type="secondary">{t('search.total', { count: data?.total ?? 0 })}</Typography.Text>
      <AsyncState loading={isLoading} error={isError ? new Error('search failed') : null} empty={data?.items.length === 0} onRetry={() => void refetch()}>
        <Space orientation="vertical" style={{ width: '100%' }}>
          {data?.items.map(hit => <Card key={hit.pageId} size="small">
            <Link to={hit.url}><Typography.Title level={4} style={{ marginTop: 0 }}><Highlight text={hit.title} term={query} /></Typography.Title></Link>
            <Typography.Paragraph><Highlight text={hit.snippet} term={query} /></Typography.Paragraph>
            <Space wrap><Tag>{hit.spaceName}</Tag>{hit.type && <Tag>{t(`docType.${hit.type}`, hit.type)}</Tag>}{hit.status && <Tag>{t(`status.${hit.status}`, hit.status)}</Tag>}<Typography.Text type="secondary">{new Date(hit.updatedAt).toLocaleString()}</Typography.Text></Space>
          </Card>)}
        </Space>
      </AsyncState>
      <Pagination current={page + 1} total={data?.total ?? 0} pageSize={20} showSizeChanger={false} onChange={p => change('page', p > 1 ? String(p - 1) : undefined)} />
    </>}
  </Space>;
}
