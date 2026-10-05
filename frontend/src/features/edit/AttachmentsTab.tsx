import { Button, List, Typography, Upload, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { api } from '../../api/client';
import { useAttachments } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

// Attachments tab for the edit page (F3/T08): list, upload, copy-markdown.
export function AttachmentsTab({ pageId, readOnly = false }: { pageId: number | null; readOnly?: boolean }) {
  const { data, isLoading, isError, refetch } = useAttachments(pageId);
  const [uploading, setUploading] = useState(false);

  const upload = async (file: File): Promise<void> => {
    if (readOnly) return;
    if (pageId === null) {
      void message.warning('先保存页面再上传附件');
      return;
    }
    setUploading(true);
    try {
      const form = new FormData();
      form.append('file', file);
      await api.post(`/api/wiki/pages/${pageId}/attachments`, form);
      await refetch();
    } catch {
      void message.error('上传失败');
    } finally {
      setUploading(false);
    }
  };

  return (
    <>
      {!readOnly && <Upload
        beforeUpload={file => {
          void upload(file);
          return false;
        }}
        showUploadList={false}
        disabled={pageId === null}
      >
        <Button icon={<UploadOutlined />} loading={uploading} disabled={pageId === null}>
          上传附件
        </Button>
      </Upload>}
      <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={(data?.length ?? 0) === 0} onRetry={() => void refetch()}>
      <List
        style={{ marginTop: 8 }}
        dataSource={data ?? []}
        renderItem={a => (
          <List.Item
            actions={[
              <Button
                key="copy"
                size="small"
                onClick={() => {
                  void navigator.clipboard?.writeText(a.markdown).then(
                    () => message.success('已复制引用'),
                    () => message.error('复制失败'),
                  );
                }}
              >
                复制引用
              </Button>,
            ]}
          >
            <List.Item.Meta title={a.fileName} description={`${a.mimeType} · ${(a.size / 1024).toFixed(1)} KB`} />
            <a href={a.url} target="_blank" rel="noopener noreferrer">
              <Typography.Link>打开</Typography.Link>
            </a>
          </List.Item>
        )}
      />
      </AsyncState>
    </>
  );
}
