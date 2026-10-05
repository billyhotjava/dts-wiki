import { Alert, Avatar, Descriptions, Tag } from 'antd';
import { Link } from 'react-router';
import { useState } from 'react';
import { api } from '../../api/client';
import type { PageView } from '../../api/hooks';

function StatusTag({ status }: { status: string | null }) {
  if (status === null) return null;
  const color = status === 'DONE' ? 'green' : status === 'IN_PROGRESS' ? 'blue' : status === 'BLOCKED' ? 'red' : 'default';
  return <Tag color={color}>{status}</Tag>;
}

function DocLink({ spaceSlug, docId, label }: { spaceSlug: string; docId: string; label?: string }) {
  const [pageId, setPageId] = useState<number | null>(null);
  const [failed, setFailed] = useState(false);
  const resolve = () => {
    if (pageId !== null || failed) return;
    api
      .get<{ pageId: number }>(`/api/wiki/spaces/${spaceSlug}/pages/by-doc-id`, { params: { docId } })
      .then(({ data }) => setPageId(data.pageId))
      .catch(() => setFailed(true));
  };
  if (failed) return <span>{label ?? docId}</span>;
  if (pageId === null) {
    return (
      <a onClick={resolve} style={{ cursor: 'pointer' }}>
        {label ?? docId}
      </a>
    );
  }
  return <Link to={`/s/${spaceSlug}/p/${pageId}`}>{label ?? docId}</Link>;
}

// Reading-state property panel (design 10 S4.3): compact Descriptions under the title.
// Pages without frontmatter (plain `page` type) show nothing.
export function PropertiesPanel({ page }: { page: PageView }) {
  const meta = page.meta;
  if (meta === null) return null;
  const hasFm = meta.docType !== 'page' || meta.docId !== null || meta.owner !== null || meta.tags.length > 0;
  if (!hasFm) return null;
  return (
    <div style={{ marginTop: 8 }}>
      {!meta.valid && (
        <Alert
          type="warning"
          showIcon
          title="属性不合规"
          description={meta.errors.map(e => `${e.path}: ${e.message}`).join('；')}
          style={{ marginBottom: 8 }}
        />
      )}
      <Descriptions size="small" column={4} bordered>
        <Descriptions.Item label="类型">{meta.docType}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <StatusTag status={meta.status} />
        </Descriptions.Item>
        <Descriptions.Item label="负责人">
          {meta.owner !== null && (
            <span>
              <Avatar size="small">{meta.owner.slice(0, 1).toUpperCase()}</Avatar> {meta.owner}
            </span>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="优先级">{meta.priority}</Descriptions.Item>
        {meta.docId !== null && (
          <Descriptions.Item label="编号" span={2}>
            {meta.docId}
          </Descriptions.Item>
        )}
        {meta.tags.length > 0 && (
          <Descriptions.Item label="标签" span={2}>
            {meta.tags.map(t => (
              <Tag key={t}>{t}</Tag>
            ))}
          </Descriptions.Item>
        )}
        {meta.depends.length > 0 && (
          <Descriptions.Item label="依赖" span={4}>
            {meta.depends.map(d => (
              <span key={d} style={{ marginRight: 8 }}>
                <DocLink spaceSlug={page.spaceSlug} docId={d} />
              </span>
            ))}
          </Descriptions.Item>
        )}
        {meta.related.length > 0 && (
          <Descriptions.Item label="相关" span={4}>
            {meta.related.map(d => (
              <span key={d} style={{ marginRight: 8 }}>
                <DocLink spaceSlug={page.spaceSlug} docId={d} />
              </span>
            ))}
          </Descriptions.Item>
        )}
      </Descriptions>
    </div>
  );
}
