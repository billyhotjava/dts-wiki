import { Button, Input, Select, Space, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { uploadAttachment, useCreatePage, usePage, useSavePageContent, useTemplates, type PageView } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';
import { MarkdownEditor, type MarkdownEditorHandle } from './MarkdownEditor';
import { VersionConflictModal } from './VersionConflictModal';

// Page editing (design 05 S4): title, save (Ctrl/Cmd+S), message, cancel;
// 409 conflict modal; images auto-upload; new pages pick a template.
// Draft autosave + presence arrive with W8 (endpoints do not exist yet).
export function PageEditorPage({ mode }: { mode: 'edit' | 'new' }) {
  const { slug = '', pageId = '' } = useParams();
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const id = mode === 'edit' ? Number(pageId) : null;

  const { data: page, isLoading, isError, refetch } = usePage(id ?? 0);
  const { data: templates } = useTemplates(slug);
  const saveContent = useSavePageContent();
  const createPage = useCreatePage(slug);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState<string | null>(null);
  const [messageText, setMessageText] = useState('');
  const [templateId, setTemplateId] = useState<string | undefined>(undefined);
  const [conflict, setConflict] = useState<number | null>(null);
  const [createdId, setCreatedId] = useState<number | null>(null);
  const editorRef = useRef<MarkdownEditorHandle>(null);
  const effectiveId = id ?? createdId;

  useEffect(() => {
    if (mode === 'edit' && page) {
      setTitle(page.title);
      setContent(page.contentMd ?? '');
    }
  }, [mode, page]);

  const currentBase = (p: PageView | undefined) => p?.versionNo ?? 0;

  const doSave = async (force = false) => {
    const body = editorRef.current?.getMarkdown() ?? content ?? '';
    try {
      if (effectiveId === null) {
        const parentId = search.get('parent') === null ? undefined : Number(search.get('parent'));
        const created = await createPage.mutateAsync({ parentId, title: title.trim() || '未命名', contentMd: body });
        setCreatedId(created.id);
        navigate(`/s/${slug}/p/${created.id}`);
      } else {
        const base = force && conflict !== null ? conflict : currentBase(page);
        await saveContent.mutateAsync({ id: effectiveId, slug, body: { baseVersionNo: base, contentMd: body, message: messageText || undefined } });
        void message.success('已保存');
        navigate(`/s/${slug}/p/${effectiveId}`);
      }
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number; data?: { currentVersionNo?: number } } }).response?.status;
      if (status === 409) {
        const serverVersion = (e as { response: { data: { currentVersionNo: number } } }).response.data.currentVersionNo;
        setConflict(serverVersion ?? null);
      } else {
        void message.error('保存失败');
      }
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        void doSave();
      }
    };
    const onUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener('keydown', onKey);
    window.addEventListener('beforeunload', onUnload);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('beforeunload', onUnload);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [content, title, messageText, createdId, page]);

  const handleUpload = async (file: File): Promise<string> => {
    let pid = effectiveId;
    if (pid === null) {
      // uploads need a page: auto-create the (still empty) page first
      const parentId = search.get('parent') === null ? undefined : Number(search.get('parent'));
      const created = await createPage.mutateAsync({ parentId, title: title.trim() || '未命名' });
      pid = created.id;
      setCreatedId(pid);
    }
    return uploadAttachment(pid, file);
  };

  if (mode === 'edit' && (isLoading || page === undefined || content === null)) {
    if (isError) {
      return <AsyncState loading={false} error={new Error('load failed')} empty={false} onRetry={() => void refetch()} children={null} />;
    }
    return <Spin fullscreen tip="Loading" />;
  }

  const initial = mode === 'new' && templateId ? (templates?.find(t => t.id === templateId)?.contentMd ?? '') : (content ?? '');

  return (
    <>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Input value={title} onChange={e => setTitle(e.target.value)} placeholder="标题" />
        {mode === 'new' && (
          <Select
            style={{ width: 280 }}
            placeholder="选择模板（可选）"
            allowClear
            value={templateId}
            onChange={setTemplateId}
            options={(templates ?? []).map(t => ({ value: t.id, label: t.title }))}
          />
        )}
        <MarkdownEditor
          key={`${mode}-${effectiveId ?? 'new'}-${templateId ?? ''}`}
          ref={editorRef}
          value={initial}
          onChange={setContent}
          onUploadImage={handleUpload}
        />
        <Space>
          <Input value={messageText} onChange={e => setMessageText(e.target.value)} placeholder="保存说明（可选）" style={{ width: 280 }} />
          <Button type="primary" loading={saveContent.isPending || createPage.isPending} onClick={() => void doSave()}>
            保存
          </Button>
          <Button onClick={() => navigate(-1)}>取消</Button>
        </Space>
        <Typography.Text type="secondary">Ctrl/⌘+S 保存 · 页内跳转前请先保存（草稿箱随 W8 到来）</Typography.Text>
      </Space>
      <VersionConflictModal
        open={conflict !== null}
        serverVersion={conflict}
        onCancel={() => setConflict(null)}
        onReload={() => {
          setConflict(null);
          void refetch();
        }}
        onOverwrite={() => {
          setConflict(null);
          void doSave(true);
        }}
      />
    </>
  );
}
