import { Dropdown, Tree, message, Modal, Input } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { useCopyPage, useCreatePage, useDeletePage, useRenameMove, useTree, type TreeNode } from '../../api/hooks';
import { AsyncState } from '../../components/AsyncState';

function toDataNodes(nodes: TreeNode[], selectedId?: number): DataNode[] {
  return nodes.map(n => ({
    key: n.id,
    title: n.title,
    children: n.children.length > 0 ? toDataNodes(n.children, selectedId) : undefined,
    isLeaf: !n.hasChildren,
  }));
}

function findNode(nodes: TreeNode[], id: number): TreeNode | undefined {
  for (const n of nodes) {
    if (n.id === id) return n;
    const hit = findNode(n.children, id);
    if (hit) return hit;
  }
  return undefined;
}

// Space page tree (design 05 S3): virtual scroll, drag-to-move, right-click menu.
export function PageTree() {
  const { slug = '', pageId } = useParams();
  const navigate = useNavigate();
  const selectedId = pageId === undefined ? undefined : Number(pageId);
  const { data, isLoading, isError, refetch } = useTree(slug);
  const renameMove = useRenameMove();
  const createPage = useCreatePage(slug);
  const copyPage = useCopyPage();
  const deletePage = useDeletePage();
  const [modal, setModal] = useState<{ mode: 'rename' | 'create' | 'copy'; node: TreeNode } | null>(null);
  const [input, setInput] = useState('');

  const openModal = (mode: 'rename' | 'create' | 'copy', node: TreeNode) => {
    setInput(mode === 'rename' ? node.title : '');
    setModal({ mode, node });
  };

  const submitModal = async () => {
    if (modal === null || input.trim() === '') return;
    try {
      if (modal.mode === 'rename') {
        await renameMove.mutateAsync({ id: modal.node.id, slug, body: { title: input.trim() } });
      } else if (modal.mode === 'create') {
        const created = await createPage.mutateAsync({ parentId: modal.node.id, title: input.trim() });
        navigate(`/s/${slug}/p/${created.id}`);
      } else {
        await copyPage.mutateAsync({ id: modal.node.id, slug, body: { targetParentId: modal.node.id, title: input.trim() } });
      }
      setModal(null);
    } catch {
      void message.error('操作失败');
    }
  };

  const doDelete = (node: TreeNode) => {
    Modal.confirm({
      title: `删除「${node.title}」？`,
      content: '页面进入回收站，子树一并删除；GIT 页将在仓库中删除对应文件。',
      okType: 'danger',
      onOk: () =>
        deletePage
          .mutateAsync({ id: node.id, slug })
          .then(() => {
            if (selectedId === node.id) navigate(`/s/${slug}`);
          })
          .catch(() => message.error('删除失败')),
    });
  };

  return (
    <>
      <AsyncState loading={isLoading} error={isError ? new Error('load failed') : null} empty={(data?.length ?? 0) === 0} onRetry={() => void refetch()}>
        <Tree
          virtual
          height={600}
          draggable
          blockNode
          showLine={false}
          selectedKeys={selectedId === undefined ? [] : [selectedId]}
          treeData={toDataNodes(data ?? [], selectedId)}
          onSelect={keys => {
            if (keys.length > 0) navigate(`/s/${slug}/p/${keys[0]}`);
          }}
          onDrop={info => {
            const dropId = Number(info.node.key);
            const dragId = Number(info.dragNode.key);
            if (Number.isNaN(dropId) || Number.isNaN(dragId)) return;
            // drop onto node => new parent; drop between => keep parent (position only, W4 keeps order)
            const newParentId = info.dropToGap ? undefined : dropId;
            if (newParentId === undefined) return;
            renameMove.mutate(
              { id: dragId, slug, body: { parentId: newParentId } },
              { onError: () => message.error('移动失败（可能是跨空间或循环移动）') },
            );
          }}
          titleRender={node => {
            const id = Number(node.key);
            const full = findNode(data ?? [], id);
            const label = full?.title ?? '';
            if (full === undefined) return <span>{label}</span>;
            return (
              <Dropdown
                menu={{
                  items: [
                    { key: 'create', label: '新建子页' },
                    { key: 'rename', label: '改名' },
                    { key: 'copy', label: '复制到本页下' },
                    { key: 'delete', label: '删除', danger: true },
                  ],
                  onClick: ({ key, domEvent }) => {
                    domEvent.stopPropagation();
                    if (key === 'create') openModal('create', full);
                    else if (key === 'rename') openModal('rename', full);
                    else if (key === 'copy') openModal('copy', full);
                    else doDelete(full);
                  },
                }}
                trigger={['contextMenu']}
              >
                <span>{label}</span>
              </Dropdown>
            );
          }}
        />
      </AsyncState>
      <Modal
        open={modal !== null}
        title={modal?.mode === 'rename' ? '改名' : modal?.mode === 'create' ? '新建子页' : '复制'}
        onOk={() => void submitModal()}
        onCancel={() => setModal(null)}
        confirmLoading={renameMove.isPending || createPage.isPending || copyPage.isPending}
      >
        <Input value={input} onChange={e => setInput(e.target.value)} onPressEnter={() => void submitModal()} placeholder="标题" />
      </Modal>
    </>
  );
}
