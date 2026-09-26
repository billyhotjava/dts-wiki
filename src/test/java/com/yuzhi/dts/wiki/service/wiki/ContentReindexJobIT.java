package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/** Reproduces the .50 reindex failure: backfill a versioned page created outside addVersion. */
@IntegrationTest
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class ContentReindexJobIT {

    @Autowired
    private ContentReindexJob job;

    @Autowired
    private PageService pageService;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private PageMetaDao pageMetaDao;

    @Autowired
    private PageVersionRepository pageVersionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void backfillsMetaAndSearchForExistingVersions() {
        Space space = spaceRepository.saveAndFlush(new Space().slug("w5rx").name("RX").archived(false));
        PageDtos.PageView root = pageService.createPage(
            "w5rx",
            new PageDtos.CreatePageRequest(null, "Doc", null, "---\ntype: task\nid: S6/F9/T01\nfeature: S6/F5\ntitle: RX\nstatus: READY\n---\n# RX\n")
        );
        pageMetaDao.deleteByPage(root.id());
        job.reindexMissing();
        assertThat(pageMetaDao.findByPage(root.id())).isPresent();
        assertThat(pageMetaDao.findByPage(root.id()).orElseThrow().docId()).isEqualTo("S6/F9/T01");
    }

    @Test
    void serviceCreatedVersionReloadsAcrossPersistenceContext() {
        Space space = spaceRepository.saveAndFlush(new Space().slug("w5cl").name("CL").archived(false));
        PageDtos.PageView root = pageService.createPage("w5cl", new PageDtos.CreatePageRequest(null, "Doc", null, "# Hello"));
        Long versionId = pageService.getPage(root.id()).versionNo() == null ? null : pageVersionRepository.findAll().get(0).getId();
        entityManager.flush();
        entityManager.clear();
        com.yuzhi.dts.wiki.domain.PageVersion reloaded = pageVersionRepository.findById(versionId).orElseThrow();
        assertThat(reloaded.getContentMd()).isEqualTo("# Hello");
    }
    @Test
    void rawRowLoadsThroughHibernate() {
        // regression: @Lob String on PostgreSQL TEXT broke every cross-transaction read
        // ("Bad value for type long"); entities now map TEXT without @Lob.
        Long spaceId = spaceRepository.saveAndFlush(new Space().slug("w5sc").name("SC").archived(false)).getId();
        Long rootId = jdbc.queryForObject(
            "INSERT INTO page (id, title, kind, position, sync_status, created_at, updated_at, space_id, parent_id) VALUES (nextval('sequence_generator'),'Root','FOLDER',1000,'LOCAL_ONLY',now(),now(),?,null) RETURNING id",
            Long.class,
            spaceId
        );
        Long pageId = jdbc.queryForObject(
            "INSERT INTO page (id, title, kind, position, sync_status, created_at, updated_at, space_id, parent_id) VALUES (nextval('sequence_generator'),'Raw','NATIVE',2000,'LOCAL_ONLY',now(),now(),?,?) RETURNING id",
            Long.class,
            spaceId,
            rootId
        );
        String body = "# Raw body here";
        Long versionId = jdbc.queryForObject(
            "INSERT INTO page_version (id, version_no, content_md, content_sha_256, author_name, source, created_at, page_id) VALUES (nextval('sequence_generator'),1,?,'abc','tester','WEB',now(),?) RETURNING id",
            Long.class,
            body,
            pageId
        );
        jdbc.update("UPDATE page SET current_version_id = ? WHERE id = ?", versionId, pageId);
        entityManager.flush();
        entityManager.clear();
        assertThat(pageVersionRepository.findById(versionId).orElseThrow().getContentMd()).isEqualTo(body);
        assertThat(entityManager.createQuery("select v.contentMd from PageVersion v where v.id = :id", String.class).setParameter("id", versionId).getSingleResult()).isEqualTo(
            body
        );
    }

    @Test
    void backfillsRawSqlSeededRows() {
        // mimics hand-seeded prod rows (page + version linked by hand, no meta)
        Long spaceId = spaceRepository.saveAndFlush(new Space().slug("w5rw").name("RW").archived(false)).getId();
        Long pageId = jdbc.queryForObject(
            "INSERT INTO page (id, title, kind, position, sync_status, created_at, updated_at, space_id, parent_id) VALUES (nextval('sequence_generator'),'Raw','NATIVE',2000,'LOCAL_ONLY',now(),now(),?,null) RETURNING id",
            Long.class,
            spaceId
        );
        String body = "---\ntype: task\nid: S6/F9/T02\nfeature: S6/F5\ntitle: Raw\nstatus: READY\n---\n# Raw\n";
        Long versionId = jdbc.queryForObject(
            "INSERT INTO page_version (id, version_no, content_md, content_sha_256, author_name, source, created_at, page_id) VALUES (nextval('sequence_generator'),1,?,'abc','tester','WEB',now(),?) RETURNING id",
            Long.class,
            body,
            pageId
        );
        jdbc.update("UPDATE page SET current_version_id = ? WHERE id = ?", versionId, pageId);
        job.reindexMissing();
        assertThat(pageMetaDao.findByPage(pageId).orElseThrow().docId()).isEqualTo("S6/F9/T02");
    }
}
