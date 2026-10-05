import { Avatar, Input, Modal, Alert } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';

export interface MentionUser {
  login: string;
  id: string;
  displayName: string;
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
  const { t } = useTranslation();
  const [failed, setFailed] = useState(false);
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<MentionUser[]>([]);

  useEffect(() => {
    if (!open) return;
    let active = true;
    if (!query.trim()) { setUsers([]); setFailed(false); return; }
    const timer = window.setTimeout(() => {
      void api
        .get<MentionUser[]>('/api/wiki/users/mention', { params: { q: query, spaceSlug } })
        .then(({ data }) => { if (active) { setUsers(data); setFailed(false); } })
        .catch(() => { if (active) { setUsers([]); setFailed(true); } });
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [open, query, spaceSlug]);

  return (
    <Modal open={open} title="@提及" onCancel={onCancel} footer={null} width={420}>
      {failed && <Alert type="warning" title={t('collaboration.identityUnavailable')} />}
      <Input.Search placeholder="搜索用户名或姓名" value={query} onChange={e => setQuery(e.target.value)} style={{ marginBottom: 8 }} />
      <ul style={{ padding: 0, listStyle: 'none' }}>{users.map(user => <li key={user.id} style={{ padding: 8 }}>
        <button type="button" onClick={() => onPick(user.login)} style={{ width: '100%', textAlign: 'left', border: 0, background: 'none', cursor: 'pointer' }}>
          <Avatar size="small">{user.displayName.slice(0, 1).toUpperCase()}</Avatar> {user.displayName} · @{user.login}
        </button>
      </li>)}</ul>
    </Modal>
  );
}
