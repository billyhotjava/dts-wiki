import { Button, Col, InputNumber, Modal, Pagination, Row, Space, Table, Tabs, Typography, message } from 'antd';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useRestoreVersion, useVersion, useVersions, type PageVersion, type PageView } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';
import { MarkdownView } from '../../components/MarkdownView';
import { lineDiff } from './lineDiff';

export function HistoryTab({ page }: { page: PageView }) {
  const { t } = useTranslation();
  const [index, setIndex] = useState(0);
  const [compare, setCompare] = useState(false);
  const [beforeNo, setBeforeNo] = useState(1);
  const [afterNo, setAfterNo] = useState(page.versionNo ?? 1);
  const history = useVersions(page.id, index);
  const before = useVersion(page.id, compare ? beforeNo : null);
  const after = useVersion(page.id, compare ? afterNo : null);
  const restore = useRestoreVersion();
  const lines = useMemo(() => lineDiff(before.data?.contentMd ?? '', after.data?.contentMd ?? ''), [before.data, after.data]);
  const additions = lines.filter(l => l.kind === 'add').length, removals = lines.filter(l => l.kind === 'remove').length;
  const canRestore = page.editable && !page.gitReadOnly;
  function open(no: number) { setBeforeNo(no === page.versionNo ? Math.max(1, no - 1) : no); setAfterNo(page.versionNo ?? no); setCompare(true); }
  function confirmRestore() {
    Modal.confirm({ title: t('history.confirm'), content: t('history.summary', { additions, removals }),
      onOk: async () => {
        try { await restore.mutateAsync({ id: page.id, no: beforeNo, baseVersionNo: page.versionNo ?? 0 }); setCompare(false); message.success(t('history.restored')); }
        catch { message.error(t('history.restoreFailed')); throw new Error('Restore failed'); }
      } });
  }
  return <>
    <AsyncState loading={history.isLoading} error={history.isError ? new Error('history failed') : null} empty={history.data?.items.length === 0} onRetry={() => void history.refetch()}>
      <Table<PageVersion> rowKey="versionNo" pagination={false} dataSource={history.data?.items} columns={[
        { title: t('history.version'), dataIndex: 'versionNo' },
        { title: t('history.author'), render: (_, row) => `${row.authorName}${row.viaAgent ? t('history.via', { agent: row.viaAgent }) : ''}` },
        { title: t('history.time'), dataIndex: 'createdAt', render: value => new Date(value as string).toLocaleString() },
        { title: t('history.message'), dataIndex: 'message' },
        { title: t('history.source'), dataIndex: 'source', render: value => t(`versionSource.${value}`, value as string) },
        { title: t('history.compare'), render: (_, row) => <Button onClick={() => open(row.versionNo)}>{t('history.compare')}</Button> },
      ]} />
      <Pagination current={index + 1} total={history.data?.total ?? 0} pageSize={20} showSizeChanger={false} onChange={n => setIndex(n - 1)} />
    </AsyncState>
    <Modal title={t('history.compare')} open={compare} width={1100} onCancel={() => setCompare(false)} footer={canRestore ? <Button loading={restore.isPending} disabled={!before.data || !after.data || beforeNo === page.versionNo} onClick={confirmRestore}>{t('history.restoreVersion', { no: beforeNo })}</Button> : null}>
      <Space><Typography.Text>{t('history.before')}</Typography.Text><InputNumber aria-label={t('history.before')} min={1} max={page.versionNo ?? 1} value={beforeNo} onChange={v => v && setBeforeNo(v)} />
        <Typography.Text>{t('history.after')}</Typography.Text><InputNumber aria-label={t('history.after')} min={1} max={page.versionNo ?? 1} value={afterNo} onChange={v => v && setAfterNo(v)} /></Space>
      <AsyncState loading={before.isLoading || after.isLoading} error={before.isError || after.isError ? new Error('version failed') : null} empty={before.data === undefined || after.data === undefined} onRetry={() => { void before.refetch(); void after.refetch(); }}>
        <Typography.Paragraph>{t('history.summary', { additions, removals })}</Typography.Paragraph>
        <Tabs items={[
          { key: 'source', label: t('history.sourceDiff'), children: <pre style={{ maxHeight: 480, overflow: 'auto', fontSize: 13 }}>{lines.map((line, n) => <div key={n} style={{ background: line.kind === 'add' ? '#e6ffed' : line.kind === 'remove' ? '#ffeef0' : undefined }}><span>{line.kind === 'add' ? '+' : line.kind === 'remove' ? '-' : ' '}</span>{line.text || ' '}</div>)}</pre> },
          { key: 'rendered', label: t('history.renderedDiff'), children: <Row gutter={16}><Col span={12}><MarkdownView content={before.data?.contentMd ?? ''} pageId={page.id} spaceSlug={page.spaceSlug} /></Col><Col span={12}><MarkdownView content={after.data?.contentMd ?? ''} pageId={page.id} spaceSlug={page.spaceSlug} /></Col></Row> },
        ]} />
      </AsyncState>
    </Modal>
  </>;
}
