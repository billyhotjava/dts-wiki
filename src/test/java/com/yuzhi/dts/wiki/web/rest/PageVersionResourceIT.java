package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.PageVersionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.service.PageVersionService;
import com.yuzhi.dts.wiki.service.mapper.PageVersionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link PageVersionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class PageVersionResourceIT {

    private static final Integer DEFAULT_VERSION_NO = 1;
    private static final Integer UPDATED_VERSION_NO = 2;
    private static final Integer SMALLER_VERSION_NO = 1 - 1;

    private static final String DEFAULT_CONTENT_MD = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT_MD = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT_SHA_256 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String UPDATED_CONTENT_SHA_256 = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private static final String DEFAULT_AUTHOR_NAME = "AAAAAAAAAA";
    private static final String UPDATED_AUTHOR_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_AUTHOR_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_AUTHOR_EMAIL = "BBBBBBBBBB";

    private static final VersionSource DEFAULT_SOURCE = VersionSource.WEB;
    private static final VersionSource UPDATED_SOURCE = VersionSource.GIT;

    private static final String DEFAULT_GIT_COMMIT = "AAAAAAAAAA";
    private static final String UPDATED_GIT_COMMIT = "BBBBBBBBBB";

    private static final String DEFAULT_MESSAGE = "AAAAAAAAAA";
    private static final String UPDATED_MESSAGE = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/page-versions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PageVersionRepository pageVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private PageVersionRepository pageVersionRepositoryMock;

    @Autowired
    private PageVersionMapper pageVersionMapper;

    @Mock
    private PageVersionService pageVersionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPageVersionMockMvc;

    private PageVersion pageVersion;

    private PageVersion insertedPageVersion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageVersion createEntity(EntityManager em) {
        PageVersion pageVersion = new PageVersion()
            .versionNo(DEFAULT_VERSION_NO)
            .contentMd(DEFAULT_CONTENT_MD)
            .contentSha256(DEFAULT_CONTENT_SHA_256)
            .authorName(DEFAULT_AUTHOR_NAME)
            .authorEmail(DEFAULT_AUTHOR_EMAIL)
            .source(DEFAULT_SOURCE)
            .gitCommit(DEFAULT_GIT_COMMIT)
            .message(DEFAULT_MESSAGE)
            .createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        pageVersion.setPage(page);
        return pageVersion;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageVersion createUpdatedEntity(EntityManager em) {
        PageVersion updatedPageVersion = new PageVersion()
            .versionNo(UPDATED_VERSION_NO)
            .contentMd(UPDATED_CONTENT_MD)
            .contentSha256(UPDATED_CONTENT_SHA_256)
            .authorName(UPDATED_AUTHOR_NAME)
            .authorEmail(UPDATED_AUTHOR_EMAIL)
            .source(UPDATED_SOURCE)
            .gitCommit(UPDATED_GIT_COMMIT)
            .message(UPDATED_MESSAGE)
            .createdAt(UPDATED_CREATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createUpdatedEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        updatedPageVersion.setPage(page);
        return updatedPageVersion;
    }

    @BeforeEach
    void initTest() {
        pageVersion = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPageVersion != null) {
            pageVersionRepository.delete(insertedPageVersion);
            insertedPageVersion = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void getAllPageVersions() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pageVersion.getId().intValue())))
            .andExpect(jsonPath("$.[*].versionNo").value(hasItem(DEFAULT_VERSION_NO)))
            .andExpect(jsonPath("$.[*].contentMd").value(hasItem(DEFAULT_CONTENT_MD)))
            .andExpect(jsonPath("$.[*].contentSha256").value(hasItem(DEFAULT_CONTENT_SHA_256)))
            .andExpect(jsonPath("$.[*].authorName").value(hasItem(DEFAULT_AUTHOR_NAME)))
            .andExpect(jsonPath("$.[*].authorEmail").value(hasItem(DEFAULT_AUTHOR_EMAIL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].gitCommit").value(hasItem(DEFAULT_GIT_COMMIT)))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageVersionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(pageVersionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageVersionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pageVersionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageVersionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pageVersionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageVersionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pageVersionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPageVersion() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get the pageVersion
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL_ID, pageVersion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pageVersion.getId().intValue()))
            .andExpect(jsonPath("$.versionNo").value(DEFAULT_VERSION_NO))
            .andExpect(jsonPath("$.contentMd").value(DEFAULT_CONTENT_MD))
            .andExpect(jsonPath("$.contentSha256").value(DEFAULT_CONTENT_SHA_256))
            .andExpect(jsonPath("$.authorName").value(DEFAULT_AUTHOR_NAME))
            .andExpect(jsonPath("$.authorEmail").value(DEFAULT_AUTHOR_EMAIL))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.gitCommit").value(DEFAULT_GIT_COMMIT))
            .andExpect(jsonPath("$.message").value(DEFAULT_MESSAGE))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getPageVersionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        Long id = pageVersion.getId();

        defaultPageVersionFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultPageVersionFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultPageVersionFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo equals to
        defaultPageVersionFiltering("versionNo.equals=" + DEFAULT_VERSION_NO, "versionNo.equals=" + UPDATED_VERSION_NO);
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo in
        defaultPageVersionFiltering("versionNo.in=" + DEFAULT_VERSION_NO + "," + UPDATED_VERSION_NO, "versionNo.in=" + UPDATED_VERSION_NO);
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo is not null
        defaultPageVersionFiltering("versionNo.specified=true", "versionNo.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo is greater than or equal to
        defaultPageVersionFiltering(
            "versionNo.greaterThanOrEqual=" + DEFAULT_VERSION_NO,
            "versionNo.greaterThanOrEqual=" + UPDATED_VERSION_NO
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo is less than or equal to
        defaultPageVersionFiltering("versionNo.lessThanOrEqual=" + DEFAULT_VERSION_NO, "versionNo.lessThanOrEqual=" + SMALLER_VERSION_NO);
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo is less than
        defaultPageVersionFiltering("versionNo.lessThan=" + UPDATED_VERSION_NO, "versionNo.lessThan=" + DEFAULT_VERSION_NO);
    }

    @Test
    @Transactional
    void getAllPageVersionsByVersionNoIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where versionNo is greater than
        defaultPageVersionFiltering("versionNo.greaterThan=" + SMALLER_VERSION_NO, "versionNo.greaterThan=" + DEFAULT_VERSION_NO);
    }

    @Test
    @Transactional
    void getAllPageVersionsByContentSha256IsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where contentSha256 equals to
        defaultPageVersionFiltering("contentSha256.equals=" + DEFAULT_CONTENT_SHA_256, "contentSha256.equals=" + UPDATED_CONTENT_SHA_256);
    }

    @Test
    @Transactional
    void getAllPageVersionsByContentSha256IsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where contentSha256 in
        defaultPageVersionFiltering(
            "contentSha256.in=" + DEFAULT_CONTENT_SHA_256 + "," + UPDATED_CONTENT_SHA_256,
            "contentSha256.in=" + UPDATED_CONTENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByContentSha256IsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where contentSha256 is not null
        defaultPageVersionFiltering("contentSha256.specified=true", "contentSha256.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByContentSha256ContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where contentSha256 contains
        defaultPageVersionFiltering(
            "contentSha256.contains=" + DEFAULT_CONTENT_SHA_256,
            "contentSha256.contains=" + UPDATED_CONTENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByContentSha256NotContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where contentSha256 does not contain
        defaultPageVersionFiltering(
            "contentSha256.doesNotContain=" + UPDATED_CONTENT_SHA_256,
            "contentSha256.doesNotContain=" + DEFAULT_CONTENT_SHA_256
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorName equals to
        defaultPageVersionFiltering("authorName.equals=" + DEFAULT_AUTHOR_NAME, "authorName.equals=" + UPDATED_AUTHOR_NAME);
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorName in
        defaultPageVersionFiltering(
            "authorName.in=" + DEFAULT_AUTHOR_NAME + "," + UPDATED_AUTHOR_NAME,
            "authorName.in=" + UPDATED_AUTHOR_NAME
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorName is not null
        defaultPageVersionFiltering("authorName.specified=true", "authorName.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorNameContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorName contains
        defaultPageVersionFiltering("authorName.contains=" + DEFAULT_AUTHOR_NAME, "authorName.contains=" + UPDATED_AUTHOR_NAME);
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorName does not contain
        defaultPageVersionFiltering("authorName.doesNotContain=" + UPDATED_AUTHOR_NAME, "authorName.doesNotContain=" + DEFAULT_AUTHOR_NAME);
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorEmailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorEmail equals to
        defaultPageVersionFiltering("authorEmail.equals=" + DEFAULT_AUTHOR_EMAIL, "authorEmail.equals=" + UPDATED_AUTHOR_EMAIL);
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorEmailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorEmail in
        defaultPageVersionFiltering(
            "authorEmail.in=" + DEFAULT_AUTHOR_EMAIL + "," + UPDATED_AUTHOR_EMAIL,
            "authorEmail.in=" + UPDATED_AUTHOR_EMAIL
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorEmailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorEmail is not null
        defaultPageVersionFiltering("authorEmail.specified=true", "authorEmail.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorEmailContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorEmail contains
        defaultPageVersionFiltering("authorEmail.contains=" + DEFAULT_AUTHOR_EMAIL, "authorEmail.contains=" + UPDATED_AUTHOR_EMAIL);
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorEmailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where authorEmail does not contain
        defaultPageVersionFiltering(
            "authorEmail.doesNotContain=" + UPDATED_AUTHOR_EMAIL,
            "authorEmail.doesNotContain=" + DEFAULT_AUTHOR_EMAIL
        );
    }

    @Test
    @Transactional
    void getAllPageVersionsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where source equals to
        defaultPageVersionFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllPageVersionsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where source in
        defaultPageVersionFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllPageVersionsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where source is not null
        defaultPageVersionFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByGitCommitIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where gitCommit equals to
        defaultPageVersionFiltering("gitCommit.equals=" + DEFAULT_GIT_COMMIT, "gitCommit.equals=" + UPDATED_GIT_COMMIT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByGitCommitIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where gitCommit in
        defaultPageVersionFiltering("gitCommit.in=" + DEFAULT_GIT_COMMIT + "," + UPDATED_GIT_COMMIT, "gitCommit.in=" + UPDATED_GIT_COMMIT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByGitCommitIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where gitCommit is not null
        defaultPageVersionFiltering("gitCommit.specified=true", "gitCommit.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByGitCommitContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where gitCommit contains
        defaultPageVersionFiltering("gitCommit.contains=" + DEFAULT_GIT_COMMIT, "gitCommit.contains=" + UPDATED_GIT_COMMIT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByGitCommitNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where gitCommit does not contain
        defaultPageVersionFiltering("gitCommit.doesNotContain=" + UPDATED_GIT_COMMIT, "gitCommit.doesNotContain=" + DEFAULT_GIT_COMMIT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByMessageIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where message equals to
        defaultPageVersionFiltering("message.equals=" + DEFAULT_MESSAGE, "message.equals=" + UPDATED_MESSAGE);
    }

    @Test
    @Transactional
    void getAllPageVersionsByMessageIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where message in
        defaultPageVersionFiltering("message.in=" + DEFAULT_MESSAGE + "," + UPDATED_MESSAGE, "message.in=" + UPDATED_MESSAGE);
    }

    @Test
    @Transactional
    void getAllPageVersionsByMessageIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where message is not null
        defaultPageVersionFiltering("message.specified=true", "message.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByMessageContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where message contains
        defaultPageVersionFiltering("message.contains=" + DEFAULT_MESSAGE, "message.contains=" + UPDATED_MESSAGE);
    }

    @Test
    @Transactional
    void getAllPageVersionsByMessageNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where message does not contain
        defaultPageVersionFiltering("message.doesNotContain=" + UPDATED_MESSAGE, "message.doesNotContain=" + DEFAULT_MESSAGE);
    }

    @Test
    @Transactional
    void getAllPageVersionsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where createdAt equals to
        defaultPageVersionFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where createdAt in
        defaultPageVersionFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPageVersionsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPageVersion = pageVersionRepository.saveAndFlush(pageVersion);

        // Get all the pageVersionList where createdAt is not null
        defaultPageVersionFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPageVersionsByAuthorIsEqualToSomething() throws Exception {
        User author;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            pageVersionRepository.saveAndFlush(pageVersion);
            author = UserResourceIT.createEntity();
        } else {
            author = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(author);
        em.flush();
        pageVersion.setAuthor(author);
        pageVersionRepository.saveAndFlush(pageVersion);
        String authorId = author.getId();
        // Get all the pageVersionList where author equals to authorId
        defaultPageVersionShouldBeFound("authorId.equals=" + authorId);

        // Get all the pageVersionList where author equals to "invalid-id"
        defaultPageVersionShouldNotBeFound("authorId.equals=" + "invalid-id");
    }

    @Test
    @Transactional
    void getAllPageVersionsByPageIsEqualToSomething() throws Exception {
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            pageVersionRepository.saveAndFlush(pageVersion);
            page = PageResourceIT.createEntity(em);
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        em.persist(page);
        em.flush();
        pageVersion.setPage(page);
        pageVersionRepository.saveAndFlush(pageVersion);
        Long pageId = page.getId();
        // Get all the pageVersionList where page equals to pageId
        defaultPageVersionShouldBeFound("pageId.equals=" + pageId);

        // Get all the pageVersionList where page equals to (pageId + 1)
        defaultPageVersionShouldNotBeFound("pageId.equals=" + (pageId + 1));
    }

    private void defaultPageVersionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPageVersionShouldBeFound(shouldBeFound);
        defaultPageVersionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPageVersionShouldBeFound(String filter) throws Exception {
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pageVersion.getId().intValue())))
            .andExpect(jsonPath("$.[*].versionNo").value(hasItem(DEFAULT_VERSION_NO)))
            .andExpect(jsonPath("$.[*].contentMd").value(hasItem(DEFAULT_CONTENT_MD)))
            .andExpect(jsonPath("$.[*].contentSha256").value(hasItem(DEFAULT_CONTENT_SHA_256)))
            .andExpect(jsonPath("$.[*].authorName").value(hasItem(DEFAULT_AUTHOR_NAME)))
            .andExpect(jsonPath("$.[*].authorEmail").value(hasItem(DEFAULT_AUTHOR_EMAIL)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].gitCommit").value(hasItem(DEFAULT_GIT_COMMIT)))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPageVersionShouldNotBeFound(String filter) throws Exception {
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPageVersionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPageVersion() throws Exception {
        // Get the pageVersion
        restPageVersionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return pageVersionRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected PageVersion getPersistedPageVersion(PageVersion pageVersion) {
        return pageVersionRepository.findById(pageVersion.getId()).orElseThrow();
    }

    protected void assertPersistedPageVersionToMatchAllProperties(PageVersion expectedPageVersion) {
        assertPageVersionAllPropertiesEquals(expectedPageVersion, getPersistedPageVersion(expectedPageVersion));
    }

    protected void assertPersistedPageVersionToMatchUpdatableProperties(PageVersion expectedPageVersion) {
        assertPageVersionAllUpdatablePropertiesEquals(expectedPageVersion, getPersistedPageVersion(expectedPageVersion));
    }
}
