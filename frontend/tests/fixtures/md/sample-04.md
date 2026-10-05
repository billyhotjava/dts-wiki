# 05 前端设计（React + antd）

## 1. 技术选型与工程结构
| 方面 | 选择 |
|------|------|
| 框架 | React 19 + TypeScript（`strict`，不允许 `allowJs`） |
| 组件库 | **antd 6**（`@ant-design/icons` 6），`ConfigProvider` 统一主题与 `zh_CN` 语言包 |
| 路由 | React Router 8（数据路由 `createBrowserRouter`） |
| 服务端状态 | TanStack Query 5（缓存、重试、乐观更新）；**不引入 Redux**，少量全局 UI 状态用 React Context |
| HTTP | axios 实例：`withCredentials`、`X-XSRF-TOKEN`（DTS Wiki 默认启用 CSRF，读 `XSRF-TOKEN` Cookie）、401 时整页跳转登录 |
| 接口类型 | 由后端 OpenAPI 生成：`openapi-typescript` 读取 `/v3/api-docs`（后端 `api-docs` profile），产物 `src/api/schema.d.ts`，不手写重复类型 |
| 国际化 | react-i18next，默认 `zh-CN`，另有 `en`；文案文件 `src/locales/{zh-CN,en}/*.json` |
| 构建 | Vite + pnpm；单元测试 Vitest + Testing Library；E2E Playwright |
| 编辑器 | Milkdown Crepe 或 Vditor（F0 spike 决定），封装为 `MarkdownEditor` |
| 渲染 | markdown-it + 插件（见 §6） |

```
frontend/
  index.html, vite.config.ts, tsconfig.json, package.json
  src/
    main.tsx, App.tsx, router.tsx, theme.ts
    api/            client.ts（axios）、schema.d.ts（生成）、hooks/*.ts（TanStack Query 封装：useSpaces、usePage、useSavePage…）
    auth/           AuthProvider.tsx（/api/account）、RequireAuth、RequireAdmin
    layout/         AppLayout.tsx（Header + Sider + Content）、SpaceSwitcher、GlobalSearch、NotificationBell、UserMenu
    features/
      tree/         PageTree.tsx（antd Tree，虚拟滚动、拖拽、右键 Dropdown）
      page/         PageView.tsx、PageHeader.tsx（Breadcrumb + 元信息 + 操作）、AttachmentsTab.tsx、LabelsEditor.tsx
      edit/         PageEditorPage.tsx、MarkdownEditor.tsx、VersionConflictModal.tsx、DraftBanner.tsx、EditorsPresence.tsx
      history/      HistoryPage.tsx（Timeline）、VersionDiff.tsx
      conflict/     ConflictMergePage.tsx
      comments/     CommentSection.tsx（Comment 列表 + Mentions 输入）
      search/       SearchPage.tsx
      home/         HomePage.tsx、SpaceHomePage.tsx、TrashPage.tsx
      admin/        SpacesAdminPage.tsx、SpaceFormDrawer.tsx、SyncAdminPage.tsx、ImportModal.tsx
    components/     MarkdownView.tsx、AsyncState.tsx（空/加载/错误）、UserAvatar.tsx、SyncBadge.tsx
    locales/        zh-CN/*.json、en/*.json
  tests/e2e/        Playwright 用例（07 §5 验收脚本）
```

## 2. 路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomePage | 可读空间卡片、我最近编辑、全站最近更新 |
| `/s/:slug` | SpaceHomePage | 空间首页（根页正文）+ 最近更新 + 同步状态 |
| `/s/:slug/p/:pageId` | PageView | 阅读 |
| `/s/:slug/p/:pageId/edit` | PageEditorPage | 编辑 |
| `/s/:slug/p/:pageId/history` | HistoryPage（`?a=&b=` 进入对比） | 历史与对比 |
| `/s/:slug/new?parent=:id` | PageEditorPage（新建） | 选模板、填标题 |
| `/s/:slug/trash` | TrashPage | 回收站 |
| `/conflicts/:id` | ConflictMergePage | 同步冲突合并 |
| `/search?q=` | SearchPage | |
| `/admin/spaces`、`/admin/sync` | SpacesAdminPage、SyncAdminPage | `RequireAdmin` |
| 旧链接 `/p/:slug/*`、`/docs/*`、`/worklog/*`、`/sandbox/*` | LegacyRedirect | 调 `/api/wiki/spaces/{slug}/resolve?path=` 后 `navigate(replace)`（映射规则见 06 §6） |

## 3. 布局（antd Layout，Confluence 式）

```
┌ Header ──────────────────────────────────────────────────────────────────────────┐
│ DTS Wiki │ <Select 空间: PRS 花卉租赁> │ <Input.Search 全局搜索> │ <Button ＋新建> │ <Badge 🔔3> │ <Dropdown 谢志民> │
├ Sider (可折叠, 宽 300) ──┬ Content ─────────────────────────────────────────────────┤
│ PRS 花卉租赁             │ <Breadcrumb 首页 / 工作日志 / v1.0.0 / sprint-1-202609>   │
│ <Tree>                    │ <Typography.Title>Sprint-1：可运行基线</Title>  [编辑] [⋯] │
│  ▸ 产品文档              │ <Space> 谢志民 · 2 小时前 · 版本 7 · <Tag git> prs-stack@abc1234 │
│  ▾ 工作日志              │ <Alert type=error 同步冲突：此页在 git 中也被修改 [去合并]>  │
│    ▾ v1.0.0              │ <Tabs 正文 | 附件(3) | 历史>                                  │
│      • sprint-1-202609   │  正文（MarkdownView）+ 右侧 <Anchor> 目录                     │
│      • sprint-queue      │ <Divider> 评论（CommentSection）                               │
│ ─────────                │                                                                │
│ 回收站 · 空间设置         │                                                                │
└──────────────────────────┴────────────────────────────────────────────────────────────────┘
```

- **页面树**：antd `Tree`（`virtual`，`height` 撑满侧栏；`draggable` 拖拽排序与改父节点，`onDrop` 前校验不可放入自身子孙）；节点标题右侧悬停显示 `Dropdown` 菜单（新建子页、改名、移动到…、复制、删除）；当前页 `selectedKeys` 高亮并展开祖先；`SyncBadge` 显示 同步中 / 冲突。
- **四态**：所有数据区块用 `AsyncState`：加载用 `Skeleton`，空用 `Empty`（带引导按钮），错误用 `Result status="error"` + 重试。
- **操作反馈**：保存、移动、删除用 `message`；删除、覆盖他人修改、恢复版本用 `Modal.confirm`（写明对 git 的影响，如"将在仓库中删除该文件"）。

## 4. 编辑（PageEditorPage）
- `MarkdownEditor` 封装（对外只暴露 Markdown 字符串）：`value`、`onChange`、`onUploadImage(file) => Promise<string>`（返回插入的 Markdown）、`readOnly`。
  必须满足：打开后不修改则不触发 `onChange`；导出内容与输入**字节一致**（F0 验收）；YAML frontmatter 不进入编辑区，保存时原样拼回。
- 页面顶部：标题 `Input`、保存按钮（Ctrl/⌘+S）、"保存说明"（可选，写入版本 message 与 git 提交信息）、取消。
- 409 版本冲突 → `VersionConflictModal`：[查看差异]（打开 VersionDiff 抽屉）[加载最新版本] [仍用我的版本覆盖]（二次确认）。
- 409 同步冲突 → 引导到 `/conflicts/:id`。
- 草稿：内容变化后每 5 s `PUT /draft`；打开时若服务端草稿比当前版本新 → `Alert` "发现 X 时间的未保存草稿 [恢复] [丢弃]"。
- 协作提示：每 30 s `POST /editing`；他人在编辑时 `Alert type=warning` 显示姓名（`Avatar.Group`）。
- 离开有未保存内容 → `useBlocker` + `beforeunload` 提示。
- 图片：粘贴/拖拽 → `POST /attachments` → 插入返回的 `markdown`；编辑器内预览 `./assets/x.png` 走 `/api/wiki/pages/{id}/raw/assets/x.png`。

## 5. 历史、对比与冲突合并
- HistoryPage：`Timeline` 列出版本（作者头像、时间、来源 `Tag`：网页 / git（提交号可点击跳 GitHub）/ 合并 / 恢复、说明）；勾选两个版本 → 对比；每个版本 [查看] [恢复到此版本]。
- VersionDiff：jsdiff 行级对比，`Segmented` 切换"并排 / 统一 / 渲染对比"。
- ConflictMergePage：上方三栏只读（基线 / wiki 版 / git 版，差异高亮），下方结果编辑区（CodeMirror 6，预填 `git merge-file` 结果，冲突块高亮），按钮 [采用 wiki 版] [采用 git 版] [保存合并结果]。

## 6. Markdown 渲染（MarkdownView）
- markdown-it + 插件：GFM 表格、任务列表、脚注、标题锚点、Mermaid（懒加载）、代码高亮（Shiki，懒加载语言）；
  选项与现网 wiki 一致：`breaks: true`、`html: false`（禁用原始 HTML），输出再经 DOMPurify。
- 链接改写：相对链接 `./x.md`、`../a/README.md` → 调 `resolve` 得页面 id 后转为站内路由（`Link`）；`./assets/*` → 附件地址；外链新窗口并加 `rel="noopener noreferrer"`。
- 正文排版：最大宽度 880 px；antd `Typography` 风格的标题与段落间距；表格横向滚动；代码块复制按钮；右侧 `Anchor` 目录（h2/h3）。

## 7. 管理后台（antd）
- SpacesAdminPage：`Table`（名称、标识、仓库、同步根、页面数、状态）+ `Drawer` 表单新建/编辑空间（`Form` 校验 slug 规则 `^[a-z][a-z0-9-]{1,30}$`）。
- SyncAdminPage：每个空间一张 `Card`：同步状态 `Badge`、最后同步时间、`lastSyncedCommit`、待推送数、冲突数、最近错误（`Typography.Paragraph` 可展开）；按钮 [立即同步] [测试连接] [查看 deploy key]（`Modal` 显示公钥 + 复制按钮 + "到 GitHub 仓库 Settings → Deploy keys 添加并勾选 Allow write access"）[首次导入]（`ImportModal`：是否导入历史、最近 N 个版本）。

## 8. 主题与体验
- `theme.ts`：`token.colorPrimary = '#2f6f5e'`，圆角 6，字体栈 `-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`；暗色模式用 `theme.darkAlgorithm`（用户菜单切换，记忆在 localStorage）。
- 可访问性：树支持键盘方向键；所有图标按钮有 `aria-label`/`Tooltip`；焦点可见。
- 响应式：窄屏（< 992 px）侧栏收起为 `Drawer`。
