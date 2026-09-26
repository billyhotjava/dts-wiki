package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.wiki.content.ContentAnalysis;
import com.yuzhi.dts.wiki.service.wiki.content.ContentService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills {@code page_meta} + {@code page_search_doc} for pages predating W5b
 * (design 10 S3.2). LENIENT: never blocks on invalid frontmatter.
 */
@Component
public class ContentReindexJob {

    private static final Logger LOG = LoggerFactory.getLogger(ContentReindexJob.class);

    private final PageRepository pageRepository;
    private final PageMetaDao pageMetaDao;
    private final SearchIndexService searchIndexService;
    private final ContentService contentService;

    public ContentReindexJob(PageRepository pageRepository, PageMetaDao pageMetaDao, SearchIndexService searchIndexService, ContentService contentService) {
        this.pageRepository = pageRepository;
        this.pageMetaDao = pageMetaDao;
        this.searchIndexService = searchIndexService;
        this.contentService = contentService;
    }

    @Scheduled(fixedDelay = 3600_000, initialDelay = 60_000)
    @SchedulerLock(name = "content-reindex", lockAtLeastFor = "1m")
    @Transactional
    public void reindexMissing() {
        Set<Long> known = new HashSet<>(pageMetaDao.allMetaPageIds());
        List<Page> pages = pageRepository.findAll().stream().filter(p -> p.getDeletedAt() == null).toList();
        int done = 0;
        for (Page page : pages) {
            if (known.contains(page.getId())) {
                continue;
            }
            PageVersion current = page.getCurrentVersion();
            if (current == null) {
                continue;
            }
            ContentAnalysis analysis = contentService.analyze(current.getContentMd(), ContentService.Mode.LENIENT, docId ->
                pageMetaDao.docIdInSpace(page.getSpace().getId(), docId, page.getId()));
            pageMetaDao.upsert(page.getSpace().getId(), page.getId(), analysis);
            searchIndexService.upsert(
                page.getId(),
                page.getSpace().getId(),
                analysis.title() == null ? page.getTitle() : analysis.title(),
                String.join(" ", analysis.tags()),
                analysis.plainText()
            );
            done++;
        }
        if (done > 0) {
            LOG.info("Content reindex backfilled {} pages", done);
        }
    }
}
