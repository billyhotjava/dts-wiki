import { Breadcrumb, Button, Space, Tag, Typography, message, Modal, Tabs } from 'antd';
import { Link, useNavigate, useParams } from 'react-router';
import { useDeletePage, usePage } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';
import { MarkdownView } from '../../components/MarkdownView';

// Page reading view (design 05 S3/S4): breadcrumb, meta, tabs (正文/附件/历史 —
// attachments and history arrive with W4-attachments/W6), delete/restore actions.
export function PageView() {
  const { slug = '', pageId = '' } = useParams();
  const id = Number(pageId);
  const navigate = useNavigate();
  const { data, isLoading, isError, refetch } = usePage(id);
  const deletePage = useDeletePage();

  const doDelete = () => {
    if (data === undefined) return;
    Modal.confirm({
      title: `删除「${data.title}」？`,
      content: '页面进入回收站；GIT 页将在仓库中删除对应文件。',
      okType: 'danger',
      onOk: () =>
        deletePage
          .mutateAsync({ id: data.id, slug })
          .then(() => navigate(`/s/${slug}`))
          .catch(() => message.error('删除失败')),
    });
  };

  return (
    <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={data === undefined} onRetry={() => void refetch()}>
      {data !== undefined && (
        <>
          <Breadcrumb items={[{ title: <Link to={`/s/${slug}`}>{slug}</Link> }, ...data.breadcrumbs.map(b => ({ title: <Link to={`/s/${slug}/p/${b.id}`}>{b.title}</Link> })), { title: data.title }]} />
          <Space align="center" style={{ marginTop: 8 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {data.title}
            </Typography.Title>
            <Tag>{data.kind}</Tag>
            <Tag>{data.syncStatus}</Tag>
            {data.versionNo !== null && <span>版本 {data.versionNo}</span>}
          </Space>
          <Space style={{ marginTop: 8 }}>
            <Button type="primary" disabled={!data.editable} onClick={() => navigate(`/s/${slug}/p/${data.id}/edit`)}>
              编辑
            </Button>
            {data.editable && (
              <Button danger onClick={doDelete}>
                删除
              </Button>
            )}
            <Button onClick={() => navigate(`/s/${slug}/trash`)}>回收站</Button>
          </Space>
          <Tabs
            style={{ marginTop: 8 }}
            items={[
              {
                key: 'content',
                label: '正文',
                children: data.contentMd === null ? <Typography.Text type="secondary">（目录页，无正文）</Typography.Text> : <MarkdownView content={data.contentMd} />,
              },
              { key: 'attachments', label: '附件', children: <Typography.Text type="secondary">附件随 W4 后续任务到来</Typography.Text> },
              { key: 'history', label: '历史', children: <Typography.Text type="secondary">历史随 W6 到来</Typography.Text> },
            ]}
          />
        </>
      )}
    </AsyncState>
  );
}
