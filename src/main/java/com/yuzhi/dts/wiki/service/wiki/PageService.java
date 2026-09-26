package com.yuzhi.dts.wiki.service.wiki;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.PageWatchRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import com.yuzhi.dts.wiki.service.wiki.content.ContentAnalysis;
import com.yuzhi.dts.wiki.service.wiki.content.ContentService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import com.yuzhi.dts.wiki.service.wiki.dto.SpaceDtos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Page tree and page lifecycle (Sprint-6 design 02, 03 S4; feature F3).
 * Transaction boundary for all page mutations. Invariants enforced here:
 * I1 single root (via DB constraint), I2 currentVersion = max, I4 gitPath unique,
 * I5 same space (no cross-space move), I6 no move under own descendant,
 * I9 same content creates no version, I10 soft-deleted pages are invisible.
 */
@Service
public class PageService {

    private static final int POSITION_STEP = 1000;

    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final SpaceRepository spaceRepository;
    private final SyncOutboxRepository syncOutboxRepository;
    private final PageWatchRepository pageWatchRepository;
    private final UserRepository userRepository;
    private final SpaceAccessService spaceAccessService;
    private final TemplateService templateService;
    private final ContentService contentService;
    private final PageMetaDao pageMetaDao;
    private final SearchIndexService searchIndexService;
    private final ObjectMapper objectMapper;

    public PageService(
        PageRepository pageRepository,
        PageVersionRepository pageVersionRepository,
        SpaceRepository spaceRepository,
        SyncOutboxRepository syncOutboxRepository,
        PageWatchRepository pageWatchRepository,
        UserRepository userRepository,
        SpaceAccessService spaceAccessService,
        TemplateService templateService,
        ContentService contentService,
        PageMetaDao pageMetaDao,
        SearchIndexService searchIndexService,
        ObjectMapper objectMapper
    ) {
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.spaceRepository = spaceRepository;
        this.syncOutboxRepository = syncOutboxRepository;
        this.pageWatchRepository = pageWatchRepository;
        this.userRepository = userRepository;
        this.spaceAccessService = spaceAccessService;
        this.templateService = templateService;
        this.contentService = contentService;
        this.pageMetaDao = pageMetaDao;
        this.searchIndexService = searchIndexService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public List<SpaceDtos.SpaceSummary> listSpaces() {
        return spaceRepository
            .findAll()
            .stream()
            .filter(space -> spaceAccessService.canRead(space.getSlug()))
            .map(space -> {
                long count = pageRepository.findLiveBySpace(space.getId()).size();
                return new SpaceDtos.SpaceSummary(space.getSlug(), space.getName(), space.getDescription(), count, null);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public SpaceDtos.SpaceDetail getSpace(String slug) {
        Space space = findVisibleSpace(slug);
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        Long rootId = pages.stream().filter(p -> p.getParent() == null).map(Page::getId).findFirst().orElse(null);
        List<SpaceDtos.SyncRootInfo> roots = space
            .getSyncRootses()
            .stream()
            .map(r -> new SpaceDtos.SyncRootInfo(r.getRepoPath(), r.getMountPage() == null ? null : r.getMountPage().getId(), Boolean.TRUE.equals(r.getEnabled())))
            .toList();
        return new SpaceDtos.SpaceDetail(space.getSlug(), space.getName(), space.getDescription(), rootId, roots);
    }

    @Transactional(readOnly = true)
    public List<SpaceDtos.TreeNode> tree(String slug) {
        Space space = findVisibleSpace(slug);
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        Map<Long, List<Page>> byParent = new LinkedHashMap<>();
        List<Page> roots = new ArrayList<>();
        for (Page page : pages) {
            if (page.getParent() == null) {
                roots.add(page);
            } else {
                byParent.computeIfAbsent(page.getParent().getId(), k -> new ArrayList<>()).add(page);
            }
        }
        Comparator<Page> order = Comparator.comparing(Page::getPosition, Comparator.nullsLast(Integer::compareTo)).thenComparing(Page::getId);
        roots.sort(order);
        byParent.values().forEach(list -> list.sort(order));
        return roots.stream().map(p -> toNode(p, byParent)).toList();
    }

    private SpaceDtos.TreeNode toNode(Page page, Map<Long, List<Page>> byParent) {
        List<Page> children = byParent.getOrDefault(page.getId(), List.of());
        return new SpaceDtos.TreeNode(
            page.getId(),
            page.getTitle(),
            page.getKind().name(),
            !children.isEmpty(),
            page.getSyncStatus().name(),
            children.stream().map(c -> toNode(c, byParent)).toList()
        );
    }

    @Transactional(readOnly = true)
    public PageDtos.PageView getPage(Long id) {
        Page page = findVisiblePage(id);
        return toView(page);
    }

    @Transactional(readOnly = true)
    public PageDtos.PageIdResult resolve(String slug, String path) {
        Space space = findVisibleSpace(slug);
        String normalized = path == null ? "" : path.strip();
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        // README.md addresses its directory's FOLDER page; a bare directory path does too.
        String dir = normalized.endsWith("/README.md") ? normalized.substring(0, normalized.length() - "/README.md".length()) : normalized;
        Optional<Page> match = pages
            .stream()
            .filter(p -> normalized.equals(p.getGitPath()))
            .findFirst()
            .or(() -> pages.stream().filter(p -> p.getKind() == PageKind.FOLDER && dir.equals(p.getGitPath())).findFirst());
        Page page = match.orElseThrow(() -> new SpaceNotVisibleException(slug + ":" + path));
        return new PageDtos.PageIdResult(page.getId());
    }

    @Transactional(readOnly = true)
    public PageDtos.PageIdResult resolveByDocId(String slug, String docId) {
        Space space = findVisibleSpace(slug);
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        return pages
            .stream()
            .flatMap(p -> pageMetaDao.findByPage(p.getId()).filter(m -> docId.equals(m.docId())).map(m -> p).stream())
            .findFirst()
            .map(p -> new PageDtos.PageIdResult(p.getId()))
            .orElseThrow(() -> new SpaceNotVisibleException(slug + "#" + docId));
    }

    /** Raw schema JSON for the F3 properties form (null when unknown). */
    @Transactional(readOnly = true)
    public String contentSchema(String type) {
        try (var in = new org.springframework.core.io.ClassPathResource("content-schemas/" + type + ".schema.json").getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<Page> trash(String slug) {
        Space space = findVisibleSpace(slug);
        return pageRepository
            .findAll()
            .stream()
            .filter(p -> p.getSpace() != null && p.getSpace().getId().equals(space.getId()) && p.getDeletedAt() != null)
            .toList();
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public PageDtos.PageView createPage(String slug, PageDtos.CreatePageRequest request) {
        spaceAccessService.requireWrite(slug);
        Space space = findVisibleSpace(slug);
        Page parent = null;
        if (request.parentId() != null) {
            parent = findVisiblePage(request.parentId());
            if (!parent.getSpace().getId().equals(space.getId())) {
                throw new IllegalArgumentException("Parent page is in another space");
            }
        }
        PageKind kind = request.kind() == null ? (isUnderSyncRoot(parent, space) ? PageKind.GIT : PageKind.NATIVE) : PageKind.valueOf(request.kind());
        Page page = new Page();
        page.setSpace(space);
        page.setParent(parent);
        page.setTitle(request.title());
        page.setKind(kind);
        page.setPosition(nextPosition(space.getId(), parent == null ? null : parent.getId()));
        page.setSyncStatus(PageSyncStatus.LOCAL_ONLY);
        page.setCreatedAt(Instant.now());
        page.setUpdatedAt(Instant.now());
        if (kind == PageKind.GIT || (kind == PageKind.FOLDER && isUnderSyncRoot(parent, space))) {
            page.setGitPath(gitPathFor(parent, space, request.title()));
        }
        page = pageRepository.save(page);
        String initialContent = request.contentMd() != null ? request.contentMd() : templateService.resolve(slug, request.templateId());
        if (initialContent != null) {
            addVersion(page, initialContent, null, VersionSource.WEB);
        }
        if (kind == PageKind.GIT) {
            markPendingPush(page, OutboxOp.WRITE, Map.of("gitPath", page.getGitPath()));
        }
        return toView(page);
    }

    @Transactional
    public PageDtos.SaveContentResult saveContent(Long id, PageDtos.SaveContentRequest request) {
        return saveContent(id, request, null);
    }

    @Transactional
    public PageDtos.SaveContentResult saveContent(Long id, PageDtos.SaveContentRequest request, String viaAgent) {
        if (viaAgent != null && !viaAgent.matches("[A-Za-z0-9._-]{1,50}")) {
            throw new IllegalArgumentException("Invalid X-Wiki-Agent");
        }
        Page page = pageRepository.findForUpdate(id).orElseThrow(() -> new SpaceNotVisibleException("page:" + id));
        spaceAccessService.requireWrite(page);
        if (page.getSyncStatus() == PageSyncStatus.CONFLICT) {
            throw new PageSyncConflictException(id);
        }
        PageVersion current = page.getCurrentVersion();
        int currentNo = current == null ? 0 : current.getVersionNo();
        if (currentNo != request.baseVersionNo()) {
            throw new PageVersionConflictException(currentNo);
        }
        String sha = sha256(request.contentMd());
        if (current != null && sha.equals(current.getContentSha256())) {
            return new PageDtos.SaveContentResult(currentNo); // I9: no new version
        }
        addVersion(page, request.contentMd(), request.message(), VersionSource.WEB, ContentService.Mode.STRICT, viaAgent);
        if (page.getKind() == PageKind.GIT) {
            markPendingPush(page, OutboxOp.WRITE, Map.of("gitPath", page.getGitPath()));
        }
        return new PageDtos.SaveContentResult(currentNo + 1);
    }

    @Transactional
    public PageDtos.PageView renameOrMove(Long id, PageDtos.UpdatePageRequest request) {
        Page page = findVisiblePage(id);
        spaceAccessService.requireWrite(page);
        if (request.title() != null && !request.title().isBlank()) {
            page.setTitle(request.title());
        }
        if (request.parentId() != null) {
            Page parent = findVisiblePage(request.parentId());
            if (!parent.getSpace().getId().equals(page.getSpace().getId())) {
                throw new IllegalArgumentException("Cannot move a page to another space"); // I5
            }
            if (isDescendantOrSelf(parent, page)) {
                throw new IllegalArgumentException("Cannot move a page under its own descendant"); // I6
            }
            page.setParent(parent);
            page.setPosition(request.position() != null ? request.position() : nextPosition(page.getSpace().getId(), parent.getId()));
            if (page.getKind() == PageKind.GIT || page.getKind() == PageKind.FOLDER) {
                repathSubtree(page);
            }
        } else if (request.position() != null) {
            page.setPosition(request.position());
        }
        page.setUpdatedAt(Instant.now());
        page = pageRepository.save(page);
        if (page.getKind() == PageKind.GIT) {
            markPendingPush(page, OutboxOp.MOVE, Map.of("gitPath", String.valueOf(page.getGitPath())));
        }
        return toView(page);
    }

    @Transactional
    public PageDtos.PageView copyPage(Long id, PageDtos.CopyPageRequest request) {
        Page source = findVisiblePage(id);
        spaceAccessService.requireWrite(source);
        Page targetParent = findVisiblePage(request.targetParentId());
        if (!targetParent.getSpace().getId().equals(source.getSpace().getId())) {
            throw new IllegalArgumentException("Cannot copy a page to another space");
        }
        Page copy = deepCopy(source, targetParent, request.title() != null ? request.title() : source.getTitle() + " (copy)");
        return toView(copy);
    }

    private Page deepCopy(Page source, Page targetParent, String title) {
        Page copy = new Page();
        copy.setSpace(source.getSpace());
        copy.setParent(targetParent);
        copy.setTitle(title);
        copy.setKind(source.getKind());
        copy.setPosition(nextPosition(source.getSpace().getId(), targetParent.getId()));
        copy.setSyncStatus(PageSyncStatus.LOCAL_ONLY);
        copy.setCreatedAt(Instant.now());
        copy.setUpdatedAt(Instant.now());
        if (source.getKind() == PageKind.GIT) {
            copy.setGitPath(uniqueGitPath(targetParentDir(targetParent), slugFilename(title)));
        }
        copy = pageRepository.save(copy);
        PageVersion current = source.getCurrentVersion();
        if (current != null) {
            addVersion(copy, current.getContentMd(), "Copied from page " + source.getId(), VersionSource.WEB);
        }
        if (copy.getKind() == PageKind.GIT) {
            markPendingPush(copy, OutboxOp.WRITE, Map.of("gitPath", copy.getGitPath()));
        }
        for (Page child : liveChildren(source)) {
            deepCopy(child, copy, child.getTitle());
        }
        return copy;
    }

    @Transactional
    public void deletePage(Long id) {
        Page page = findVisiblePage(id);
        spaceAccessService.requireWrite(page);
        Instant now = Instant.now();
        deleteSubtree(page, now);
        if (page.getKind() == PageKind.GIT) {
            markPendingPush(page, OutboxOp.DELETE, Map.of("gitPath", String.valueOf(page.getGitPath())));
        }
    }

    private void deleteSubtree(Page page, Instant now) {
        page.setDeletedAt(now);
        page.setUpdatedAt(now);
        pageRepository.save(page);
        pageMetaDao.deleteByPage(page.getId());
        searchIndexService.delete(page.getId());
        for (Page child : liveChildren(page)) {
            deleteSubtree(child, now);
        }
    }

    @Transactional
    public PageDtos.PageView restorePage(Long id) {
        Page page = pageRepository.findById(id).orElseThrow(() -> new SpaceNotVisibleException("page:" + id));
        spaceAccessService.requireWrite(page.getSpace() == null ? null : page.getSpace().getSlug());
        restoreSubtree(page);
        if (page.getKind() == PageKind.GIT) {
            markPendingPush(page, OutboxOp.RESTORE, Map.of("gitPath", String.valueOf(page.getGitPath())));
        }
        return toView(page);
    }

    private void restoreSubtree(Page page) {
        page.setDeletedAt(null);
        page.setUpdatedAt(Instant.now());
        pageRepository.save(page);
        // LENIENT: restores must never be blocked by tightened schemas.
        PageVersion current = page.getCurrentVersion();
        if (current != null) {
            ContentAnalysis analysis = contentService.analyze(current.getContentMd(), ContentService.Mode.LENIENT, docId ->
                pageMetaDao.docIdInSpace(page.getSpace().getId(), docId, page.getId()));
            pageMetaDao.upsert(page.getSpace().getId(), page.getId(), analysis);
            searchIndexService.upsert(page.getId(), page.getSpace().getId(), analysis.title() == null ? page.getTitle() : analysis.title(), String.join(" ", analysis.tags()), analysis.plainText());
        }
        for (Page child : allChildren(page)) {
            if (child.getDeletedAt() != null) {
                restoreSubtree(child);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Repository-relative directory of a page, for attachment gitPaths (W5). */
    public String pageDir(Page page) {
        return targetParentDir(page.getParent());
    }

    /** Outbox entry for non-content page events (W5 attachments). */
    public void markGitOutbox(Page page, OutboxOp op, String gitPath) {
        markPendingPush(page, op, Map.of("gitPath", String.valueOf(gitPath)));
    }

    private Space findVisibleSpace(String slug) {
        Space space = spaceRepository.findOneBySlug(slug).orElseThrow(() -> new SpaceNotVisibleException(slug));
        spaceAccessService.requireRead(slug);
        return space;
    }

    private Page findVisiblePage(Long id) {
        Page page = pageRepository.findLive(id).orElseThrow(() -> new SpaceNotVisibleException("page:" + id));
        spaceAccessService.requireRead(page);
        return page;
    }

    private PageDtos.PageView toView(Page page) {
        PageVersion current = page.getCurrentVersion();
        List<SpaceDtos.Breadcrumb> crumbs = new ArrayList<>();
        Page cursor = page.getParent();
        while (cursor != null) {
            crumbs.add(0, new SpaceDtos.Breadcrumb(cursor.getId(), cursor.getTitle()));
            cursor = cursor.getParent();
        }
        List<String> labels = page.getLabelses().stream().map(l -> l.getName()).sorted().toList();
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        boolean watching = login != null && pageWatchRepository.existsByPageIdAndUserLogin(page.getId(), login);
        PageDtos.MetaView meta = pageMetaDao
            .findByPage(page.getId())
            .map(m ->
                new PageDtos.MetaView(
                    m.docType(),
                    m.docId(),
                    m.status(),
                    m.owner(),
                    m.priority(),
                    m.tags(),
                    m.depends(),
                    m.related(),
                    m.valid(),
                    m.errors(),
                    m.meta()
                )
            )
            .orElse(null);
        return new PageDtos.PageView(
            page.getId(),
            page.getSpace().getSlug(),
            page.getTitle(),
            page.getKind().name(),
            current == null ? null : current.getContentMd(),
            current == null ? null : current.getVersionNo(),
            page.getUpdatedAt(),
            current == null ? null : current.getAuthorName(),
            page.getGitPath(),
            page.getSpace().getGitRepoUrl(),
            current == null ? null : current.getGitCommit(),
            page.getSyncStatus().name(),
            crumbs,
            labels,
            watching,
            spaceAccessService.canWrite(page),
            meta,
            "/s/" + page.getSpace().getSlug() + "/p/" + page.getId()
        );
    }

    private void addVersion(Page page, String contentMd, String message, VersionSource source) {
        addVersion(page, contentMd, message, source, ContentService.Mode.STRICT, null);
    }

    private void addVersion(Page page, String contentMd, String message, VersionSource source, ContentService.Mode mode, String viaAgent) {
        // DTS-MD v1 content contract (design 10 S3.1): analyze first (STRICT web saves
        // reject invalid frontmatter with 422), then project to page_meta + search doc.
        ContentAnalysis analysis = contentService.analyze(
            contentMd,
            mode,
            docId -> pageMetaDao.docIdInSpace(page.getSpace().getId(), docId, page.getId() == null ? -1L : page.getId())
        );
        PageVersion current = page.getCurrentVersion();
        int next = current == null ? 1 : current.getVersionNo() + 1;
        String login = SecurityUtils.getCurrentUserLogin().orElse("unknown");
        PageVersion version = new PageVersion();
        version.setPage(page);
        version.setVersionNo(next);
        version.setContentMd(contentMd);
        version.setContentSha256(sha256(contentMd));
        version.setAuthorName(login);
        version.setSource(source);
        version.setMessage(message);
        version.setViaAgent(viaAgent);
        version.setCreatedAt(Instant.now());
        userRepository.findOneByLogin(login).ifPresent(version::setAuthor);
        version = pageVersionRepository.save(version);
        page.setCurrentVersion(version);
        page.setUpdatedAt(Instant.now());
        pageRepository.save(page);
        // flush first: page_meta has an FK to page(id) and JPA defers inserts.
        pageRepository.flush();
        pageMetaDao.upsert(page.getSpace().getId(), page.getId(), analysis);
        searchIndexService.upsert(
            page.getId(),
            page.getSpace().getId(),
            analysis.title() == null ? page.getTitle() : analysis.title(),
            String.join(" ", analysis.tags()) + " " + (analysis.docId() == null ? "" : analysis.docId()),
            analysis.plainText()
        );
    }

    private void markPendingPush(Page page, OutboxOp op, Map<String, String> extra) {
        page.setSyncStatus(PageSyncStatus.PENDING_PUSH);
        pageRepository.save(page);
        Map<String, Object> payload = new LinkedHashMap<>(extra);
        payload.put("pageId", page.getId());
        if (page.getCurrentVersion() != null) {
            payload.put("versionId", page.getCurrentVersion().getId());
        }
        SyncOutbox outbox = new SyncOutbox();
        outbox.setSpace(page.getSpace());
        outbox.setPage(page);
        outbox.setOp(op);
        outbox.setPayload(toJson(payload));
        String login = SecurityUtils.getCurrentUserLogin().orElse("unknown");
        outbox.setActorLogin(login);
        Optional<User> user = userRepository.findOneByLogin(login);
        user.ifPresent(u -> {
            outbox.setActorName(u.getFirstName());
            outbox.setActorEmail(u.getEmail());
        });
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setCreatedAt(Instant.now());
        syncOutboxRepository.save(outbox);
    }

    private boolean isUnderSyncRoot(Page parent, Space space) {
        Page cursor = parent;
        while (cursor != null) {
            final Page node = cursor;
            boolean mounted = space.getSyncRootses().stream().anyMatch(r -> Boolean.TRUE.equals(r.getEnabled()) && r.getMountPage() != null && r.getMountPage().getId().equals(node.getId()));
            if (mounted) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private String targetParentDir(Page parent) {
        if (parent == null) {
            return "";
        }
        if (parent.getKind() == PageKind.FOLDER && parent.getGitPath() != null) {
            return parent.getGitPath();
        }
        String gp = parent.getGitPath();
        if (gp == null) {
            return targetParentDir(parent.getParent());
        }
        int slash = gp.lastIndexOf('/');
        return slash < 0 ? "" : gp.substring(0, slash);
    }

    private String gitPathFor(Page parent, Space space, String title) {
        String dir = targetParentDir(parent);
        String base = slugFilename(title);
        return uniqueGitPath(dir, base);
    }

    private String uniqueGitPath(String dir, String base) {
        return dir.isEmpty() ? base : dir + "/" + base;
    }

    /** Filename generation (design 03 S7): keep Chinese, replace illegal chars, cap length. */
    static String slugFilename(String title) {
        String name = title.replaceAll("[/\\\\:*?\"<>|\\p{Cntrl}]+", "-").strip().replaceAll("[.\\s]+$", "");
        if (name.isEmpty()) {
            name = "untitled";
        }
        if (name.length() > 120) {
            name = name.substring(0, 120);
        }
        if (!name.toLowerCase().endsWith(".md")) {
            name += ".md";
        }
        return name;
    }

    private void repathSubtree(Page page) {
        String dir = targetParentDir(page.getParent());
        String old = page.getGitPath();
        String filename = old == null || !old.contains("/") ? slugFilename(page.getTitle()) : old.substring(old.lastIndexOf('/') + 1);
        page.setGitPath(dir.isEmpty() ? filename : dir + "/" + filename);
        pageRepository.save(page);
        for (Page child : allChildren(page)) {
            if (child.getKind() == PageKind.GIT || child.getKind() == PageKind.FOLDER) {
                repathChild(child, page);
            }
        }
    }

    private void repathChild(Page child, Page parent) {
        String dir = parent.getGitPath() != null && parent.getKind() == PageKind.FOLDER
            ? parent.getGitPath()
            : targetParentDir(parent);
        String old = child.getGitPath();
        String filename = old == null || !old.contains("/") ? slugFilename(child.getTitle()) : old.substring(old.lastIndexOf('/') + 1);
        child.setGitPath(dir.isEmpty() ? filename : dir + "/" + filename);
        pageRepository.save(child);
        for (Page grand : allChildren(child)) {
            if (grand.getKind() == PageKind.GIT || grand.getKind() == PageKind.FOLDER) {
                repathChild(grand, child);
            }
        }
    }

    private List<Page> liveChildren(Page page) {
        return allChildren(page).stream().filter(c -> c.getDeletedAt() == null).toList();
    }

    private List<Page> allChildren(Page page) {
        return pageRepository.findLiveBySpace(page.getSpace().getId()).stream().filter(c -> c.getParent() != null && c.getParent().getId().equals(page.getId())).toList();
    }

    private boolean isDescendantOrSelf(Page node, Page ancestor) {
        Page cursor = node;
        while (cursor != null) {
            if (cursor.getId().equals(ancestor.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private int nextPosition(Long spaceId, Long parentId) {
        return pageRepository.maxSiblingPosition(spaceId, parentId) + POSITION_STEP;
    }

    static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
