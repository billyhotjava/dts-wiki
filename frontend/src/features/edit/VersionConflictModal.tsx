import { Button, Modal, Space, Typography } from 'antd';

// 409 version conflict (design 05 S4): view diff lives in W6 History;
// here: reload latest or force-overwrite with a second confirmation.
export function VersionConflictModal({
  open,
  serverVersion,
  onReload,
  onOverwrite,
  onCancel,
}: {
  open: boolean;
  serverVersion: number | null;
  onReload: () => void;
  onOverwrite: () => void;
  onCancel: () => void;
}) {
  const confirmOverwrite = () => {
    Modal.confirm({
      title: '确定用你的版本覆盖吗？',
      content: '他人的修改将被覆盖，该操作会产生一个新版本。',
      okType: 'danger',
      onOk: onOverwrite,
    });
  };
  return (
    <Modal open={open} title="版本冲突" onCancel={onCancel} footer={null}>
      <Typography.Paragraph>有人在你编辑期间保存了新版本{serverVersion !== null ? `（当前 v${serverVersion}）` : ''}。</Typography.Paragraph>
      <Space>
        <Button onClick={onReload}>加载最新版本</Button>
        <Button danger onClick={confirmOverwrite}>
          仍用我的版本覆盖
        </Button>
      </Space>
    </Modal>
  );
}
