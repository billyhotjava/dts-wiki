import { Alert, Button, Input, Segmented, Select, Space, Spin, Tabs, Typography, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { uploadAttachment, useCreatePage, usePage, useSavePageContent, useTemplates, type PageView } from '../../api/hooks';
import { splitFrontmatter } from '../../utils/frontmatter';
import { AsyncState } from '../../components/AsyncState';
import { AttachmentsTab } from './AttachmentsTab';
import { MarkdownEditor, type MarkdownEditorHandle } from './MarkdownEditor';
import { MentionPicker } from './MentionPicker';
import { minimalDiff } from './minimalDiff';
import { PropertiesForm } from './PropertiesForm';
import { SourceEditor, type SourceEditorHandle } from './SourceEditor';
import { VersionConflictModal } from './VersionConflictModal';

// Page editing (design 05 S4 + 10 S4.3): title, properties (frontmatter), save
// (Ctrl/Cmd+S), message, cancel; 409 conflict modal; 422 frontmatter errors;
// images auto-upload; new pages pick a template.
// Draft autosave + presence arrive with W8 (endpoints do not exist yet).
// NOTE (W5b): body is saved as the editor serializes it; minimalDiff block-level
// preservation arrives with W5c (F1) — see 10 S4.1.
export function PageEditorPage({ mode }: { mode: 'edit' | 'new' }) {
  const { slug = '', pageId = '' } = useParams();
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const id = mode === 'edit' ? Number(pageId) : null;

  const { data: page, isLoading, isError, refetch } = usePage(id ?? 0);
  const { data: templates } = useTemplates(slug);
  const saveContent = useSavePageContent();
  const createPage = useCreatePage(slug);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState<string | null>(null);
  const [frontmatter, setFrontmatter] = useState('');
  const [messageText, setMessageText] = useState('');
  const [templateId, setTemplateId] = useState<string | undefined>(undefined);
  const [conflict, setConflict] = useState<number | null>(null);
  const [frontmatterErrors, setFrontmatterErrors] = useState<string[]>([]);
  const [createdId, setCreatedId] = useState<number | null>(null);
  const [sourceMode, setSourceMode] = useState(false);
  const [sourceInit, setSourceInit] = useState('');
  const [mentionOpen, setMentionOpen] = useState(false);
  const editorRef = useRef<MarkdownEditorHandle>(null);
  const sourceRef = useRef<SourceEditorHandle>(null);
  const effectiveId = id ?? createdId;

  useEffect(() => {
    if (mode === 'edit' && page) {
      setTitle(page.title);
      const split = splitFrontmatter(page.contentMd ?? '');
      setFrontmatter(split.front);
      setContent(split.body);
    }
  }, [mode, page]);

  const currentBase = (p: PageView | undefined) => p?.versionNo ?? 0;

  const fullNow = (): string => {
    if (sourceMode) {
      return sourceRef.current?.getMarkdown() ?? frontmatter + (content ?? '');
    }
    return editorRef.current?.getMarkdown() ?? frontmatter + (content ?? '');
  };

  const toggleSourceMode = () => {
    const { front, body } = splitFrontmatter(fullNow());
    setFrontmatter(front);
    setContent(body);
    setSourceInit(front + body);
    setSourceMode(!sourceMode);
  };

  const insertMention = (login: string) => {
    setMentionOpen(false);
    const text = `@${login} `;
    if (sourceMode) {
      sourceRef.current?.insertText(text);
    } else {
      editorRef.current?.insertText(text);
    }
  };

  // editor emits full markdown (frontmatter + body); the form owns frontmatter,
  // so only the body part is tracked here.
  const handleEditorChange = (full: string) => {
    setContent(splitFrontmatter(full).body);
  };

  const composeSaveBody = (): string => {
    // design 10 S4.1: only blocks the user touched leave the editor's serialization;
    // everything else keeps original bytes (or the template for new pages).
    // Source mode edits raw text: stored verbatim, bypassing minimalDiff.
    if (sourceMode) {
      return sourceRef.current?.getMarkdown() ?? content ?? '';
    }
    const editorFull = editorRef.current?.getMarkdown() ?? content ?? '';
    const editorBody = splitFrontmatter(editorFull).body;
    if (mode === 'new') {
      return frontmatter + editorBody;
    }
    const originalBody = splitFrontmatter(page?.contentMd ?? '').body;
    return frontmatter + minimalDiff(originalBody, editorBody);
  };

  const doSave = async (force = false) => {
    if (mode === 'edit' && (!page?.editable || page.gitReadOnly)) return;
    const body = composeSaveBody();
    try {
      if (effectiveId === null) {
        const parentId = search.get('parent') === null ? undefined : Number(search.get('parent'));
        const created = await createPage.mutateAsync({ parentId, title: title.trim() || '未命名', contentMd: body });
        setCreatedId(created.id);
        navigate(`/s/${slug}/p/${created.id}`);
      } else {
        const base = force && conflict !== null ? conflict : currentBase(page);
        await saveContent.mutateAsync({ id: effectiveId, slug, body: { baseVersionNo: base, contentMd: body, message: messageText || undefined } });
        setFrontmatterErrors([]);
        void message.success('已保存');
        navigate(`/s/${slug}/p/${effectiveId}`);
      }
    } catch (e: unknown) {
      const response = (e as { response?: { status?: number; data?: { currentVersionNo?: number; errors?: { path: string; message: string }[] } } }).response;
      if (response?.status === 409) {
        setConflict(response.data?.currentVersionNo ?? null);
      } else if (response?.status === 422) {
        setFrontmatterErrors((response.data?.errors ?? []).map(err => `${err.path}: ${err.message}`));
      } else {
        void message.error('保存失败');
      }
    }
  };

  useEffect(() => {
    if (mode === 'edit' && (!page?.editable || page.gitReadOnly)) return;
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

  const handleSourceChange = (full: string) => {
    const { front, body } = splitFrontmatter(full);
    setFrontmatter(front);
    setContent(body);
  };

  const handleUpload = async (file: File): Promise<string> => {
    if (mode === 'edit' && (!page?.editable || page.gitReadOnly)) throw new Error('Page is read-only');
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
    return <Spin fullscreen description="Loading" />;
  }

  if (mode === 'edit' && page && (page.gitReadOnly || !page.editable)) {
    return <Alert type="info" showIcon title={page.gitReadOnly ? t('page.gitReadOnly') : t('page.cannotEdit')} description={page.gitReadOnly ? t('page.gitReadOnlyHint') : undefined} action={<Button onClick={() => navigate(`/s/${slug}/p/${page.id}`, { replace: true })}>{t('page.returnToPage')}</Button>} />;
  }

  const templateBody = mode === 'new' && templateId ? splitFrontmatter(templates?.find(t => t.id === templateId)?.contentMd ?? '').body : null;
  const initial = mode === 'new' ? (templateBody ?? '') : (content ?? '');

  return (
    <>
      <Space orientation="vertical" style={{ width: '100%' }} size="middle">
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
        <PropertiesForm initialYaml={frontmatter} onChange={setFrontmatter} />
        {frontmatterErrors.length > 0 && <Alert type="error" showIcon title="属性不合规，内容未保存" description={frontmatterErrors.join('；')} />}
        <Tabs
          items={[
            {
              key: 'content',
              label: '正文',
              children: (
                <>
                  <Space style={{ marginBottom: 8 }}>
                    <Segmented
                      value={sourceMode ? 'source' : 'wysiwyg'}
                      onChange={v => (v === 'source' ? !sourceMode && toggleSourceMode() : sourceMode && toggleSourceMode())}
                      options={[{ value: 'wysiwyg', label: '所见即所得' }, { value: 'source', label: '源码' }]}
                    />
                    <Button onMouseDown={e => e.preventDefault()} onClick={() => setMentionOpen(true)}>
                      ＠提及
                    </Button>
                  </Space>
                  {sourceMode ? (
                    <SourceEditor key={`src-${effectiveId ?? 'new'}`} ref={sourceRef} value={sourceInit} onChange={handleSourceChange} />
                  ) : (
                    <MarkdownEditor
                      key={`${mode}-${effectiveId ?? 'new'}-${templateId ?? ''}`}
                      ref={editorRef}
                      value={initial}
                      onChange={handleEditorChange}
                      onUploadImage={handleUpload}
                      pageId={effectiveId ?? undefined}
                    />
                  )}
                </>
              ),
            },
            { key: 'attachments', label: '附件', children: <AttachmentsTab pageId={effectiveId} /> },
          ]}
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
      <MentionPicker open={mentionOpen} spaceSlug={slug} onPick={insertMention} onCancel={() => setMentionOpen(false)} />
    </>
  );
}
