import { Avatar, Input, List, Modal } from 'antd';
import { useEffect, useState } from 'react';
import { api } from '../../api/client';

export interface MentionUser {
  login: string;
  name: string;
}

// @mention picker (design 10 S4.2 E9): search wiki users, insert `@login ` as plain
// text at the editor cursor. Rendered as a user card by MarkdownView (W8 refines).
export function MentionPicker({
  open,
  spaceSlug,
  onPick,
  onCancel,
}: {
  open: boolean;
  spaceSlug: string;
  onPick: (login: string) => void;
  onCancel: () => void;
}) {
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<MentionUser[]>([]);

  useEffect(() => {
    if (!open) return;
    const timer = window.setTimeout(() => {
      void api
        .get<MentionUser[]>('/api/wiki/users/mention', { params: { q: query, spaceSlug } })
        .then(({ data }) => setUsers(data))
        .catch(() => setUsers([]));
    }, 200);
    return () => window.clearTimeout(timer);
  }, [open, query, spaceSlug]);

  return (
    <Modal open={open} title="@提及" onCancel={onCancel} footer={null} width={420}>
      <Input.Search placeholder="搜索用户名或姓名" value={query} onChange={e => setQuery(e.target.value)} style={{ marginBottom: 8 }} />
      <List
        dataSource={users}
        renderItem={u => (
          <List.Item style={{ cursor: 'pointer' }} onClick={() => onPick(u.login)}>
            <List.Item.Meta avatar={<Avatar size="small">{u.name.slice(0, 1).toUpperCase()}</Avatar>} title={u.name} description={`@${u.login}`} />
          </List.Item>
        )}
      />
    </Modal>
  );
}
