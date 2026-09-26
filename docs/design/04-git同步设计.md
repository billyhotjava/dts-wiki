# 04 Git 双向同步设计

## 1. 原则
1. **PG 是事实源**，但同步根内的文件在 git 中也可以被开发者/AI 直接修改；两边任何一边的修改都要在 ≤ 60 s 内到达另一边。
2. **永不静默覆盖**：自上次同步点以来两边都改了同一文件 → 冲突，保留两版，由人合并。
3. **永不改写历史**：不 `--force` 推送；只会丢弃工作副本中**尚未推送**的本地提交（它们可由 outbox 重建）；wiki 的每个用户操作是一个独立提交，作者 = wiki 用户。
4. **同步器是工作副本的唯一操作者**：每个仓库一个本地 clone，只有 `GitSyncScheduler`（ShedLock 单实例）在同一时刻操作它。
5. 同步单位是**文件**；一个文件的问题不阻塞其他文件。

## 2. 配置与工作副本

| 项 | 值 |
|----|----|
| 空间 → 仓库 | `Space.gitRepoUrl`（SSH）、`gitBranch`（默认 `main`） |
| 同步根 | `SyncRoot.repoPath`（如 `docs`、`worklog`），挂载页 `mountPage`（FOLDER） |
| 工作副本 | `${repos-dir}/<space-slug>`：`git clone --branch <branch> --single-branch <url>`（全量历史，用于 `log --follow`） |
| SSH | `${ssh-keys-dir}/<space-slug>.key`（ed25519，0600）；`GIT_SSH_COMMAND="ssh -i <key> -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=${ssh-keys-dir}/known_hosts"` |
| 提交者 | `-c user.name=DTS Wiki -c user.email=wiki@yuzhicloud.com`；作者 `--author="<显示名> <邮箱>"`（邮箱缺失用 `<login>@users.noreply.yuzhicloud.com`） |
| 初始空间 | `dts` → `git@github.com:billyhotjava/dts-rdc.git`，同步根 `docs`、`worklog`；`prs` → `git@github.com:billyhotjava/prs-stack.git`，同步根 `worklog`（`docs` 待确认） |

git 调用封装在 `GitRepoManager`，所有命令：`ProcessBuilder` + 超时（fetch/push 60 s，其余 30 s）+ 捕获 stderr；非 0 退出抛 `GitCommandException(command, exitCode, stderr)`。

## 3. 页面同步状态机

```
                  网页保存/改名/移动/删除（写 outbox）
   ┌──────────┐ ─────────────────────────────────────► ┌──────────────┐
   │  SYNCED  │                                         │ PENDING_PUSH │
   └──────────┘ ◄───────────── 推送成功 ─────────────── └──────────────┘
        │  ▲                                                    │
入站：git 改了且 wiki 未改（写 GIT 版本，仍为 SYNCED）          │ 入站：git 也改了同一文件
        │  │ 解决冲突后推送成功                                  ▼
        │  └──────────────────────────────────────────── ┌──────────┐
        └── 入站：git 改了，wiki 有未推送修改 ──────────► │ CONFLICT │
                                                          └──────────┘
```

- `CONFLICT` 页：普通保存被拒（409 `PAGE_SYNC_CONFLICT`），只能走冲突合并；该页的 outbox 条目标为 `BLOCKED_BY_CONFLICT`。
- NATIVE/TEMPLATE 页恒为 `LOCAL_ONLY`，不参与同步。

## 4. 同步周期（每个启用同步的空间，每 30 s）

```
1. lock(space)                                  -- ShedLock 名称 git-sync-<slug>
2. ensureClone()                                -- 不存在则 clone
3. git fetch origin <branch>
4. Inbound(origin/<branch>)                     -- §5，先吸收远端变化（可能产生冲突）
5. git merge --ff-only origin/<branch>          -- 工作副本前移到远端（此时本地无未推送提交）
6. Outbound()                                   -- §6，处理 outbox，逐条提交
7. git push origin HEAD:<branch>
     成功 → 推进 SyncState.lastSyncedCommit = HEAD；相关页面 PENDING_PUSH→SYNCED；outbox DONE
     被拒（远端又有新提交）→ git fetch → git rebase origin/<branch> → 再 push
                            → rebase 冲突：git rebase --abort，再 git reset --hard origin/<branch>
                              （只丢弃本地尚未推送的提交；outbox 条目仍是 PENDING，下一周期从步骤 3 重做，
                               届时入站会先把远端的新改动吸收或判为冲突）
8. 写 SyncState（status、时间、错误信息）
```

要点：**本地未推送的提交只是 outbox 的"物化结果"，可以随时丢弃重建**——真正的待办永远在 outbox 表里。因此推送失败的恢复策略统一为"丢弃本地提交、回到远端、下周期重放 outbox"，简单可靠。

## 5. 入站（git → wiki）

```
changes = git diff --name-status -M50% <lastSyncedCommit> origin/<branch> -- <各同步根>
（lastSyncedCommit 为空 = 首次导入，见 §8）
for each change:
  A (新增) / M (修改) path.md:
      gitContent = git show origin/<branch>:path；sha = sha256(gitContent)
      page = findByGitPath(path)（不存在则按目录创建 FOLDER 链与 GIT 页）
      if page.currentVersion.sha == sha → 跳过（例如 wiki 自己推上去的提交）
      elif page.syncStatus == SYNCED → 新增版本(source=GIT, gitCommit=该文件最后一次提交, 作者=提交作者)
      elif page.syncStatus == PENDING_PUSH →
           base = lastSyncedCommit 时该文件的内容
           尝试三方合并（§7）：成功 → 新增 MERGE 版本，outbox 中原 WRITE 替换为合并结果，保持 PENDING_PUSH
                               失败 → 创建 SyncConflict(git 内容, wikiVersion=当前版本, baseVersion)，状态 CONFLICT
      elif CONFLICT → 更新该冲突记录的 git 内容（保留最新）
  D (删除) path:
      SYNCED → 页面软删除（ActivityEvent: 来自 git 的删除）
      PENDING_PUSH → 冲突（git 删了、wiki 改了），合并界面提供"保留 wiki 版本（恢复文件）/ 接受删除"
  R (重命名) old→new:
      更新 page.gitPath（及 FOLDER 链），不产生新版本；若内容同时变化按 M 处理
  README.md 的 A/M/D → 作用于其目录对应的 FOLDER 页正文
  非 md 文件（图片、pdf…）→ Attachment 的增删改（BlobStore 存内容）
推进 lastSyncedCommit = origin/<branch>
```

- 作者映射：`git log -1 --format=%an%x00%ae <commit> -- path`；按 `jhi_user.email`（忽略大小写）或 `login` 匹配。
- 排除：`.git*`、隐藏文件、超过 `max-file-size` 的文件（记录日志与同步状态告警）。

## 6. 出站（wiki → git）

outbox 按 `id` 顺序处理，每条一个提交：

| op | 工作副本操作 | 提交信息 |
|----|--------------|----------|
| WRITE | 写 `payload.versionId` 的内容到 `gitPath`（README 类 FOLDER 写 `<dir>/README.md`） | `wiki: edit <path>` / `wiki: create <path>` |
| MOVE | `git mv <fromPath> <toPath>`（目录整体移动；目标父目录不存在则先创建） | `wiki: move <from> -> <to>` |
| DELETE | `git rm -r <path>` | `wiki: delete <path>` |
| RESTORE | 同 WRITE（从回收站恢复） | `wiki: restore <path>` |
| ATTACH / DETACH | 从 BlobStore 写出文件 / `git rm` | `wiki: upload <path>` / `wiki: remove <path>` |

- 提交前 `git add -A -- <涉及路径>`；无实际变化（内容相同）则跳过提交，条目 DONE。
- **路径白名单**：所有写入路径必须位于该空间启用的同步根之内，否则条目 FAILED 并告警（防御性校验）。
- 失败处理：单条命令失败 → `attempts+1`、`lastError`，指数退避（30 s、1 min、2 min… 上限 30 min）；超过 10 次 → FAILED，状态页红色。

## 7. 三方合并与冲突解决
- 自动合并：`git merge-file -p <wiki版> <base> <git版>`（在临时目录执行）；退出码 0 = 无冲突。
- 人工合并界面（05 §5）提供：基线、wiki 版、git 版三栏；结果编辑区预填 `merge-file` 的输出（含 `<<<<<<<` 标记）。
- 解决：`POST /api/wiki/conflicts/{id}/resolve {contentMd, resolution}` → 新增 MERGE 版本（或 KEPT_WIKI/KEPT_GIT）→ 页面 `PENDING_PUSH` → outbox WRITE → 下一周期推送；冲突记录 `resolvedAt/resolvedBy`。
- 通知：冲突产生时通知该页最后的 wiki 编辑者与关注者（`SYNC_CONFLICT`）。

## 8. 首次导入（ImportService）
1. 管理员在 `/admin/sync` 为空间配置仓库与同步根，添加 deploy key，"测试连接"通过；
2. `POST /api/wiki/admin/import/{slug}`：clone → 为每个同步根创建挂载 FOLDER 页 → 遍历文件按 02 §5 映射创建页面与附件；
3. 版本：默认每个文件导入**最近 20 个历史版本**（`git log --follow --format=%H -n 20 -- path`，逐个 `git show <sha>:<path>`，作者/时间取提交信息）；`importHistory=false` 时只导入当前版本；
4. 排序：同目录按文件名自然排序（数字按数值、中文按拼音，与现网 wiki 一致）；
5. `lastSyncedCommit = HEAD`；输出导入报告（文件数、页面数、附件数、跳过项）。

**切换前置条件**：现网 .50 `/data/dts-wiki/repo` 中网页编辑产生的本地提交必须已推送到 GitHub（需要 dts-rdc 的写权限 deploy key），否则这些编辑不会出现在新 wiki。

## 9. 与开发者 / AI 协作的约定（写入各仓库 CLAUDE.md / README）
- 同步根内的文件随时可以直接在 git 中修改；wiki 会在 1 分钟内吸收。
- 大规模重构（批量改名、移动目录）建议在 git 中一次提交完成，wiki 入站按 rename 处理，页面 id 与历史保留。
- 不要在同步根内提交超大二进制文件（> 20 MB 不会同步到 wiki）。

## 10. 测试场景（必须全部自动化，见 07）
新增 / 修改 / 删除 / 改名 / 目录改名 / 图片 / README ↔ FOLDER / 两边改不同行（自动合并）/ 两边改同一行（冲突）/ git 删 wiki 改 / 推送被拒后重放 / 网络中断重试 / 重复执行幂等 / 中文路径。
