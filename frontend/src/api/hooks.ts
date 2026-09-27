import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './client';

export interface TreeNode {
  id: number;
  title: string;
  kind: string;
  hasChildren: boolean;
  syncStatus: string;
  children: TreeNode[];
}

export interface Breadcrumb {
  id: number;
  title: string;
}

export interface SpaceSummary {
  slug: string;
  name: string;
  description: string;
  pageCount: number;
  syncStatus: string | null;
}

export interface SpaceDetail extends SpaceSummary {
  rootPageId: number | null;
  syncRoots: { repoPath: string; mountPageId: number | null; enabled: boolean }[];
}

export interface PageMeta {
  docType: string;
  docId: string | null;
  status: string | null;
  owner: string | null;
  priority: string | null;
  tags: string[];
  depends: string[];
  related: string[];
  valid: boolean;
  errors: { path: string; message: string }[];
  fields: Record<string, unknown>;
}

export interface PageView {
  id: number;
  spaceSlug: string;
  title: string;
  kind: string;
  contentMd: string | null;
  versionNo: number | null;
  updatedAt: string;
  updatedBy: string | null;
  gitPath: string | null;
  gitRepoUrl: string | null;
  gitCommit: string | null;
  syncStatus: string;
  breadcrumbs: Breadcrumb[];
  labels: string[];
  watching: boolean;
  editable: boolean;
  meta: PageMeta | null;
  url: string;
}

export function useSpaces() {
  return useQuery({ queryKey: ['spaces'], queryFn: async () => (await api.get<SpaceSummary[]>('/api/wiki/spaces')).data, staleTime: 30_000 });
}

export interface Bootstrap {
  account: { login: string; firstName?: string | null; lastName?: string | null; email?: string | null };
  spaces: SpaceSummary[];
  canCreateSpace: boolean;
}

/** Single startup call (design 10 S3.3): replaces /api/account + /spaces round trips. */
export function useBootstrap() {
  return useQuery({
    queryKey: ['bootstrap'],
    queryFn: async () => (await api.get<Bootstrap>('/api/wiki/bootstrap')).data,
    staleTime: 30_000,
    retry: false,
  });
}

export function useSpace(slug: string) {
  return useQuery({
    queryKey: ['space', slug],
    queryFn: async () => (await api.get<SpaceDetail>(`/api/wiki/spaces/${slug}`)).data,
  });
}

export function useTree(slug: string) {
  return useQuery({
    queryKey: ['tree', slug],
    queryFn: async () => (await api.get<TreeNode[]>(`/api/wiki/spaces/${slug}/tree`)).data,
  });
}

export function usePage(id: number) {
  return useQuery({
    queryKey: ['page', id],
    queryFn: async () => (await api.get<PageView>(`/api/wiki/pages/${id}`)).data,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });
}

function invalidateSpace(queryClient: ReturnType<typeof useQueryClient>, slug: string) {
  void queryClient.invalidateQueries({ queryKey: ['tree', slug] });
  void queryClient.invalidateQueries({ queryKey: ['spaces'] });
}

export function useCreatePage(slug: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: { parentId?: number; title: string; kind?: string; templateId?: string; contentMd?: string }) =>
      (await api.post<PageView>(`/api/wiki/spaces/${slug}/pages`, body)).data,
    onSuccess: () => invalidateSpace(queryClient, slug),
  });
}

export function useRenameMove() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (args: { id: number; slug: string; body: { title?: string; parentId?: number; position?: number } }) =>
      (await api.patch<PageView>(`/api/wiki/pages/${args.id}`, args.body)).data,
    onSuccess: (_data, args) => invalidateSpace(queryClient, args.slug),
  });
}

export function useCopyPage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (args: { id: number; slug: string; body: { targetParentId: number; title?: string } }) =>
      (await api.post<PageView>(`/api/wiki/pages/${args.id}/copy`, args.body)).data,
    onSuccess: (_data, args) => invalidateSpace(queryClient, args.slug),
  });
}

export function useDeletePage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (args: { id: number; slug: string }) => api.delete(`/api/wiki/pages/${args.id}`),
    onSuccess: (_data, args) => invalidateSpace(queryClient, args.slug),
  });
}

export function useRestorePage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (args: { id: number; slug: string }) => (await api.post<PageView>(`/api/wiki/pages/${args.id}/restore`)).data,
    onSuccess: (_data, args) => {
      invalidateSpace(queryClient, args.slug);
      void queryClient.invalidateQueries({ queryKey: ['trash', args.slug] });
    },
  });
}

export function useTrash(slug: string) {
  return useQuery({
    queryKey: ['trash', slug],
    queryFn: async () => (await api.get<number[]>(`/api/wiki/spaces/${slug}/trash`)).data,
  });
}

export interface TemplateItem {
  id: string;
  title: string;
  contentMd: string;
}

export function useTemplates(slug?: string) {
  return useQuery({
    queryKey: ['templates', slug ?? ''],
    queryFn: async () => (await api.get<TemplateItem[]>('/api/wiki/templates', { params: slug ? { space: slug } : {} })).data,
  });
}

export function useSavePageContent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (args: { id: number; slug: string; body: { baseVersionNo: number; contentMd: string; message?: string } }) =>
      (await api.put<{ versionNo: number }>(`/api/wiki/pages/${args.id}/content`, args.body)).data,
    onSuccess: (_data, args) => {
      void queryClient.invalidateQueries({ queryKey: ['page', args.id] });
      void queryClient.invalidateQueries({ queryKey: ['tree', args.slug] });
    },
  });
}

export interface AttachmentItem {
  id: number;
  fileName: string;
  mimeType: string;
  size: number;
  url: string;
  markdown: string;
}

export function useAttachments(pageId: number | null) {
  return useQuery({
    queryKey: ['attachments', pageId],
    queryFn: async () => (await api.get<AttachmentItem[]>(`/api/wiki/pages/${pageId}/attachments`)).data,
    enabled: pageId !== null,
    staleTime: 30_000,
  });
}

export async function uploadAttachment(pageId: number, file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<{ markdown: string }>(`/api/wiki/pages/${pageId}/attachments`, form);
  return data.markdown;
}
