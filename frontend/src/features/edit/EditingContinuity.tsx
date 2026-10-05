import { Alert, Button, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';

export interface PrivateDraft { contentMd: string; baseVersionNo: number; updatedAt: string }
interface Props {
  pageId: number | null; enabled: boolean; baseVersionNo: number; initialMarkdown: string;
  getContent: () => string; onRecover: (draft: PrivateDraft) => void;
}
export function EditingContinuity(props: Props) {
  const { t } = useTranslation(); const latest = useRef(props); latest.current = props;
  const [draft, setDraft] = useState<PrivateDraft | null>(null), [others, setOthers] = useState<string[]>([]);
  const [failed, setFailed] = useState(false), [saved, setSaved] = useState<string | null>(null);
  useEffect(() => {
    if (!props.enabled || props.pageId === null) return;
    const id = props.pageId; let active = true, saving = false, previous = '';
    const heartbeat = () => { void api.post<{ displayName: string }[]>(`/api/wiki/pages/${id}/editing`).then(result => { if (active) setOthers(result.data.map(editor => editor.displayName)); }).catch(() => { if (active) setFailed(true); }); };
    void api.get<PrivateDraft | null>(`/api/wiki/pages/${id}/draft`).then(result => { if (active) setDraft(result.data); }).catch(() => { if (active) setFailed(true); });
    heartbeat();
    const presenceTimer = window.setInterval(heartbeat, 30000);
    const draftTimer = window.setInterval(() => {
      if (saving) return;
      const state = latest.current, markdown = state.getContent();
      if (markdown === state.initialMarkdown || markdown === previous) return;
      saving = true;
      void api.put(`/api/wiki/pages/${id}/draft`, { baseVersionNo: state.baseVersionNo, contentMd: markdown })
        .then(() => { previous = markdown; if (active) { setSaved(new Date().toLocaleTimeString()); setFailed(false); } })
        .catch(() => { if (active) setFailed(true); }).finally(() => { saving = false; });
    }, 10000);
    return () => { active = false; window.clearInterval(presenceTimer); window.clearInterval(draftTimer); void api.delete(`/api/wiki/pages/${id}/editing`).catch(() => undefined); };
  }, [props.enabled, props.pageId]);
  if (!props.enabled || props.pageId === null) return null;
  return <Space orientation="vertical" style={{ width: '100%' }}>
    {draft && <Alert type="info" title={t('editing.recoverable')} description={new Date(draft.updatedAt).toLocaleString()} action={<Space>
      <Button onClick={() => { props.onRecover(draft); setDraft(null); }}>{t('editing.recover')}</Button>
      <Button onClick={() => void api.delete(`/api/wiki/pages/${props.pageId}/draft`).then(() => setDraft(null)).catch(() => setFailed(true))}>{t('editing.discard')}</Button>
    </Space>} />}
    {others.length > 0 && <Alert type="info" title={t('editing.presence', { names: others.join(', ') })} />}
    {failed && <Alert type="warning" title={t('editing.failed')} />}
    <Typography.Text type="secondary">{saved ? t('editing.saved', { time: saved }) : t('editing.hint')}</Typography.Text>
  </Space>;
}
