import { Breadcrumb, Button, Card, Col, Dropdown, Row, Space, Tag, Typography, message, Modal, Tabs } from 'antd';
import { EllipsisOutlined, FileTextOutlined, FolderOutlined } from '@ant-design/icons';
import { Link, useNavigate, useParams } from 'react-router';
import { useDeletePage, usePage, useTree } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';
import { MarkdownView } from '../../components/MarkdownView';
import { useTranslation } from 'react-i18next';
import { AttachmentsTab } from '../edit/AttachmentsTab';
import { PropertiesPanel } from './PropertiesPanel';

// Page reading view (design 05 S3/S4 + 10 S4.6): breadcrumb, meta, tabs, ⋯ menu.
// Internal enums are not shown as text: kind as icon, syncStatus only when abnormal.
export function PageView() {
  const { slug = '', pageId = '' } = useParams();
  const id = Number(pageId);
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { data, isLoading, isError, refetch } = usePage(id);
  const { data: tree } = useTree(slug);
  const deletePage = useDeletePage();

  const doDelete = () => {
    if (data === undefined) return;
    Modal.confirm({
      title: `删除「${data.title}」？`,
      content: '页面进入回收站。',
      okType: 'danger',
      onOk: () =>
        deletePage
          .mutateAsync({ id: data.id, slug })
          .then(() => navigate(`/s/${slug}`))
          .catch(() => message.error('删除失败')),
    });
  };

  const findChildren = (nodes: NonNullable<typeof tree>, target: number): NonNullable<typeof tree> => {
    for (const n of nodes) {
      if (n.id === target) return n.children;
      const hit = findChildren(n.children, target);
      if (hit.length > 0) return hit;
    }
    return [];
  };

  return (
    <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={data === undefined} onRetry={() => void refetch()}>
      {data !== undefined && (
        <Card className="reading" style={{ maxWidth: 928 }}>
          <Breadcrumb items={[{ title: <Link to={`/s/${slug}`}>{slug}</Link> }, ...data.breadcrumbs.map(b => ({ title: <Link to={`/s/${slug}/p/${b.id}`}>{b.title}</Link> })), { title: data.title }]} />
          <Space align="center" style={{ marginTop: 8 }}>
            {data.kind === 'FOLDER' ? <FolderOutlined /> : <FileTextOutlined />}
            <Typography.Title level={3} style={{ margin: 0 }}>
              {data.title}
            </Typography.Title>
            {data.gitReadOnly && <Tag>{t('page.gitReadOnly')}</Tag>}
            {data.syncStatus === 'PENDING_PUSH' && <Tag color="blue">同步中</Tag>}
            {data.syncStatus === 'CONFLICT' && <Tag color="red">冲突</Tag>}
            {data.versionNo !== null && <span>版本 {data.versionNo}</span>}
          </Space>
          <PropertiesPanel page={data} />
          <Space style={{ marginTop: 8 }}>
            {!data.gitReadOnly && <Button type="primary" disabled={!data.editable} onClick={() => navigate(`/s/${slug}/p/${data.id}/edit`)}>
              编辑
            </Button>}
            {!data.gitReadOnly && <Dropdown
              menu={{
                items: [{ key: 'delete', label: '删除', danger: true, disabled: !data.editable }],
                onClick: ({ key }) => {
                  if (key === 'delete') doDelete();
                },
              }}
            >
              <Button icon={<EllipsisOutlined />} />
            </Dropdown>}
            <Button onClick={() => navigate(`/s/${slug}/trash`)}>回收站</Button>
          </Space>
          <Tabs
            style={{ marginTop: 8 }}
            items={[
              {
                key: 'content',
                label: '正文',
                children:
                  data.contentMd === null ? (
                    <ChildCards slug={slug} children={findChildren(tree ?? [], data.id)} />
                  ) : (
                    <MarkdownView content={data.contentMd} pageId={data.id} spaceSlug={slug} />
                  ),
              },
              { key: 'attachments', label: '附件', children: <AttachmentsTab pageId={data.id} readOnly={data.gitReadOnly || !data.editable} /> },
              { key: 'history', label: '历史', children: <Typography.Text type="secondary">历史随 W6 到来</Typography.Text> },
            ]}
          />
        </Card>
      )}
    </AsyncState>
  );
}

function ChildCards({ slug, children }: { slug: string; children: { id: number; title: string }[] }) {
  if (children.length === 0) {
    return <Typography.Text type="secondary">空目录</Typography.Text>;
  }
  return (
    <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
      {children.map(c => (
        <Col key={c.id} span={8}>
          <Link to={`/s/${slug}/p/${c.id}`}>
            <Card title={c.title} hoverable size="small" />
          </Link>
        </Col>
      ))}
    </Row>
  );
}
