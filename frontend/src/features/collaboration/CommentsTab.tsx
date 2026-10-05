import { Alert, Button, Input, Modal, Pagination, Space, Tag, Typography } from 'antd';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import { MarkdownView } from '../../components/MarkdownView';
import { MentionPicker } from '../edit/MentionPicker';

interface Comment { id: number; parentId: number | null; bodyMd: string; authorLogin: string; authorName: string; createdAt: string; deleted: boolean; resolved: boolean; canEdit: boolean; replies: Comment[] }
export function CommentsTab({ pageId, spaceSlug }: { pageId: number; spaceSlug: string }) {
  const { t } = useTranslation(); const client = useQueryClient();
  const [page, setPage] = useState(0), [body, setBody] = useState(''), [replyTo, setReplyTo] = useState<number | null>(null);
  const [editing, setEditing] = useState<Comment | null>(null), [editBody, setEditBody] = useState(''), [mention, setMention] = useState(false);
  const comments = useQuery({ queryKey: ['comments', pageId, page], queryFn: async () => (await api.get<{ items: Comment[]; total: number }>(`/api/wiki/pages/${pageId}/comments`, { params: { page: String(page), size: '20' } })).data });
  const refresh = () => { void client.invalidateQueries({ queryKey: ['comments', pageId] }); void client.invalidateQueries({ queryKey: ['personal', pageId] }); };
  const write = useMutation({ mutationFn: async (action: { kind: 'create' | 'edit' | 'delete' | 'resolve'; id?: number; resolved?: boolean }) => {
    if (action.kind === 'create') await api.post(`/api/wiki/pages/${pageId}/comments`, { bodyMd: body, parentId: replyTo });
    else if (action.kind === 'delete') await api.delete(`/api/wiki/comments/${action.id}`);
    else await api.patch(`/api/wiki/comments/${action.id}`, action.kind === 'edit' ? { bodyMd: editBody } : { resolved: action.resolved });
  }, onSuccess: (_result, action) => { refresh(); if (action.kind === 'create') { setBody(''); setReplyTo(null); } if (action.kind === 'edit') setEditing(null); } });
  function row(comment: Comment, reply = false) {
    return <section key={comment.id} style={{ padding: 12, marginLeft: reply ? 24 : 0, borderBottom: '1px solid #eee' }}>
      <Space><Typography.Text strong>{comment.authorName}</Typography.Text><Typography.Text type="secondary">{new Date(comment.createdAt).toLocaleString()}</Typography.Text>{comment.resolved && <Tag color="green">{t('collaboration.resolved')}</Tag>}</Space>
      {comment.deleted ? <p>{t('collaboration.deletedComment')}</p> : <MarkdownView content={comment.bodyMd} pageId={pageId} spaceSlug={spaceSlug} />}
      <Space>
        {!reply && !comment.deleted && <Button size="small" onClick={() => { setReplyTo(comment.id); document.getElementById(`comment-input-${pageId}`)?.focus(); }}>{t('collaboration.reply')}</Button>}
        {comment.canEdit && <><Button size="small" onClick={() => { setEditing(comment); setEditBody(comment.bodyMd); }}>{t('collaboration.edit')}</Button>
          <Button size="small" danger onClick={() => Modal.confirm({ title: t('collaboration.deleteConfirm'), onOk: () => write.mutateAsync({ kind: 'delete', id: comment.id }) })}>{t('collaboration.delete')}</Button>
          {!reply && <Button size="small" onClick={() => write.mutate({ kind: 'resolve', id: comment.id, resolved: !comment.resolved })}>{t(comment.resolved ? 'collaboration.reopen' : 'collaboration.resolve')}</Button>}</>}
      </Space>
      {comment.replies.map(child => row(child, true))}
    </section>;
  }
  return <>
    {(comments.isError || write.isError) && <Alert type="error" title={t('collaboration.failed')} action={<Button onClick={() => { write.reset(); void comments.refetch(); }}>{t('collaboration.retry')}</Button>} />}
    {comments.data?.items.map(comment => row(comment))}
    {comments.data?.items.length === 0 && <p>{t('collaboration.noComments')}</p>}
    <Pagination current={page + 1} pageSize={20} total={comments.data?.total ?? 0} showSizeChanger={false} onChange={number => setPage(number - 1)} />
    {replyTo !== null && <Space>{t('collaboration.replying')}<Button onClick={() => setReplyTo(null)}>{t('collaboration.cancel')}</Button></Space>}
    <Input.TextArea id={`comment-input-${pageId}`} value={body} onChange={event => setBody(event.target.value)} maxLength={20000} rows={4} placeholder={t('collaboration.commentPlaceholder')} />
    <Space style={{ marginTop: 8 }}><Button onClick={() => setMention(true)}>{t('collaboration.mention')}</Button><Button type="primary" loading={write.isPending} disabled={!body.trim()} onClick={() => write.mutate({ kind: 'create' })}>{t('collaboration.submit')}</Button></Space>
    <MentionPicker open={mention} spaceSlug={spaceSlug} onCancel={() => setMention(false)} onPick={login => { setBody(value => `${value} @${login} `); setMention(false); }} />
    <Modal open={editing !== null} title={t('collaboration.edit')} onCancel={() => setEditing(null)} onOk={() => write.mutate({ kind: 'edit', id: editing?.id })} confirmLoading={write.isPending} okButtonProps={{ disabled: !editBody.trim() }}>
      <Input.TextArea value={editBody} onChange={event => setEditBody(event.target.value)} maxLength={20000} rows={5} />
    </Modal>
  </>;
}
