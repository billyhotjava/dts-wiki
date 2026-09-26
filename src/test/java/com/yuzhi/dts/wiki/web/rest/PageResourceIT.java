package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.PageAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Label;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.PageService;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.mapper.PageMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
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
 * Integration tests for the {@link PageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class PageResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final PageKind DEFAULT_KIND = PageKind.FOLDER;
    private static final PageKind UPDATED_KIND = PageKind.GIT;

    private static final String DEFAULT_GIT_PATH = "AAAAAAAAAA";
    private static final String UPDATED_GIT_PATH = "BBBBBBBBBB";

    private static final Integer DEFAULT_POSITION = 1;
    private static final Integer UPDATED_POSITION = 2;
    private static final Integer SMALLER_POSITION = 1 - 1;

    private static final PageSyncStatus DEFAULT_SYNC_STATUS = PageSyncStatus.LOCAL_ONLY;
    private static final PageSyncStatus UPDATED_SYNC_STATUS = PageSyncStatus.SYNCED;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final Instant DEFAULT_DELETED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DELETED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/pages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PageRepository pageRepository;

    @Mock
    private PageRepository pageRepositoryMock;

    @Autowired
    private PageMapper pageMapper;

    @Mock
    private PageService pageServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPageMockMvc;

    private Page page;

    private Page insertedPage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Page createEntity(EntityManager em) {
        Page page = new Page()
            .title(DEFAULT_TITLE)
            .kind(DEFAULT_KIND)
            .gitPath(DEFAULT_GIT_PATH)
            .position(DEFAULT_POSITION)
            .syncStatus(DEFAULT_SYNC_STATUS)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .deletedAt(DEFAULT_DELETED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        page.setSpace(space);
        return page;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Page createUpdatedEntity(EntityManager em) {
        Page updatedPage = new Page()
            .title(UPDATED_TITLE)
            .kind(UPDATED_KIND)
            .gitPath(UPDATED_GIT_PATH)
            .position(UPDATED_POSITION)
            .syncStatus(UPDATED_SYNC_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .deletedAt(UPDATED_DELETED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createUpdatedEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        updatedPage.setSpace(space);
        return updatedPage;
    }

    @BeforeEach
    void initTest() {
        page = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPage != null) {
            pageRepository.delete(insertedPage);
            insertedPage = null;
        }
    }

    @Test
    @Transactional
    void createPage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);
        var returnedPageDTO = om.readValue(
            restPageMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PageDTO.class
        );

        // Validate the Page in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPage = pageMapper.toEntity(returnedPageDTO);
        assertPageUpdatableFieldsEquals(returnedPage, getPersistedPage(returnedPage));

        insertedPage = returnedPage;
    }

    @Test
    @Transactional
    void createPageWithExistingId() throws Exception {
        // Create the Page with an existing ID
        page.setId(1L);
        PageDTO pageDTO = pageMapper.toDto(page);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setTitle(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setKind(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPositionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setPosition(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSyncStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setSyncStatus(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setCreatedAt(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUpdatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        page.setUpdatedAt(null);

        // Create the Page, which fails.
        PageDTO pageDTO = pageMapper.toDto(page);

        restPageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPages() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList
        restPageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(page.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].gitPath").value(hasItem(DEFAULT_GIT_PATH)))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION)))
            .andExpect(jsonPath("$.[*].syncStatus").value(hasItem(DEFAULT_SYNC_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].deletedAt").value(hasItem(DEFAULT_DELETED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(pageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPage() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get the page
        restPageMockMvc
            .perform(get(ENTITY_API_URL_ID, page.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(page.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.gitPath").value(DEFAULT_GIT_PATH))
            .andExpect(jsonPath("$.position").value(DEFAULT_POSITION))
            .andExpect(jsonPath("$.syncStatus").value(DEFAULT_SYNC_STATUS.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.deletedAt").value(DEFAULT_DELETED_AT.toString()));
    }

    @Test
    @Transactional
    void getPagesByIdFiltering() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        Long id = page.getId();

        defaultPageFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultPageFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultPageFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllPagesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where title equals to
        defaultPageFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllPagesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where title in
        defaultPageFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllPagesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where title is not null
        defaultPageFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where title contains
        defaultPageFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllPagesByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where title does not contain
        defaultPageFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllPagesByKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where kind equals to
        defaultPageFiltering("kind.equals=" + DEFAULT_KIND, "kind.equals=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllPagesByKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where kind in
        defaultPageFiltering("kind.in=" + DEFAULT_KIND + "," + UPDATED_KIND, "kind.in=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    void getAllPagesByKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where kind is not null
        defaultPageFiltering("kind.specified=true", "kind.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByGitPathIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where gitPath equals to
        defaultPageFiltering("gitPath.equals=" + DEFAULT_GIT_PATH, "gitPath.equals=" + UPDATED_GIT_PATH);
    }

    @Test
    @Transactional
    void getAllPagesByGitPathIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where gitPath in
        defaultPageFiltering("gitPath.in=" + DEFAULT_GIT_PATH + "," + UPDATED_GIT_PATH, "gitPath.in=" + UPDATED_GIT_PATH);
    }

    @Test
    @Transactional
    void getAllPagesByGitPathIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where gitPath is not null
        defaultPageFiltering("gitPath.specified=true", "gitPath.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByGitPathContainsSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where gitPath contains
        defaultPageFiltering("gitPath.contains=" + DEFAULT_GIT_PATH, "gitPath.contains=" + UPDATED_GIT_PATH);
    }

    @Test
    @Transactional
    void getAllPagesByGitPathNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where gitPath does not contain
        defaultPageFiltering("gitPath.doesNotContain=" + UPDATED_GIT_PATH, "gitPath.doesNotContain=" + DEFAULT_GIT_PATH);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position equals to
        defaultPageFiltering("position.equals=" + DEFAULT_POSITION, "position.equals=" + UPDATED_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position in
        defaultPageFiltering("position.in=" + DEFAULT_POSITION + "," + UPDATED_POSITION, "position.in=" + UPDATED_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position is not null
        defaultPageFiltering("position.specified=true", "position.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position is greater than or equal to
        defaultPageFiltering("position.greaterThanOrEqual=" + DEFAULT_POSITION, "position.greaterThanOrEqual=" + UPDATED_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position is less than or equal to
        defaultPageFiltering("position.lessThanOrEqual=" + DEFAULT_POSITION, "position.lessThanOrEqual=" + SMALLER_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position is less than
        defaultPageFiltering("position.lessThan=" + UPDATED_POSITION, "position.lessThan=" + DEFAULT_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesByPositionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where position is greater than
        defaultPageFiltering("position.greaterThan=" + SMALLER_POSITION, "position.greaterThan=" + DEFAULT_POSITION);
    }

    @Test
    @Transactional
    void getAllPagesBySyncStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where syncStatus equals to
        defaultPageFiltering("syncStatus.equals=" + DEFAULT_SYNC_STATUS, "syncStatus.equals=" + UPDATED_SYNC_STATUS);
    }

    @Test
    @Transactional
    void getAllPagesBySyncStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where syncStatus in
        defaultPageFiltering("syncStatus.in=" + DEFAULT_SYNC_STATUS + "," + UPDATED_SYNC_STATUS, "syncStatus.in=" + UPDATED_SYNC_STATUS);
    }

    @Test
    @Transactional
    void getAllPagesBySyncStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where syncStatus is not null
        defaultPageFiltering("syncStatus.specified=true", "syncStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where createdAt equals to
        defaultPageFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where createdAt in
        defaultPageFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where createdAt is not null
        defaultPageFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where updatedAt equals to
        defaultPageFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where updatedAt in
        defaultPageFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where updatedAt is not null
        defaultPageFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByDeletedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where deletedAt equals to
        defaultPageFiltering("deletedAt.equals=" + DEFAULT_DELETED_AT, "deletedAt.equals=" + UPDATED_DELETED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByDeletedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where deletedAt in
        defaultPageFiltering("deletedAt.in=" + DEFAULT_DELETED_AT + "," + UPDATED_DELETED_AT, "deletedAt.in=" + UPDATED_DELETED_AT);
    }

    @Test
    @Transactional
    void getAllPagesByDeletedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        // Get all the pageList where deletedAt is not null
        defaultPageFiltering("deletedAt.specified=true", "deletedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPagesByCurrentVersionIsEqualToSomething() throws Exception {
        PageVersion currentVersion;
        if (TestUtil.findAll(em, PageVersion.class).isEmpty()) {
            pageRepository.saveAndFlush(page);
            currentVersion = PageVersionResourceIT.createEntity(em);
        } else {
            currentVersion = TestUtil.findAll(em, PageVersion.class).get(0);
        }
        em.persist(currentVersion);
        em.flush();
        page.setCurrentVersion(currentVersion);
        pageRepository.saveAndFlush(page);
        Long currentVersionId = currentVersion.getId();
        // Get all the pageList where currentVersion equals to currentVersionId
        defaultPageShouldBeFound("currentVersionId.equals=" + currentVersionId);

        // Get all the pageList where currentVersion equals to (currentVersionId + 1)
        defaultPageShouldNotBeFound("currentVersionId.equals=" + (currentVersionId + 1));
    }

    @Test
    @Transactional
    void getAllPagesByLabelsIsEqualToSomething() throws Exception {
        Label labels;
        if (TestUtil.findAll(em, Label.class).isEmpty()) {
            pageRepository.saveAndFlush(page);
            labels = LabelResourceIT.createEntity();
        } else {
            labels = TestUtil.findAll(em, Label.class).get(0);
        }
        em.persist(labels);
        em.flush();
        page.addLabels(labels);
        pageRepository.saveAndFlush(page);
        Long labelsId = labels.getId();
        // Get all the pageList where labels equals to labelsId
        defaultPageShouldBeFound("labelsId.equals=" + labelsId);

        // Get all the pageList where labels equals to (labelsId + 1)
        defaultPageShouldNotBeFound("labelsId.equals=" + (labelsId + 1));
    }

    @Test
    @Transactional
    void getAllPagesBySpaceIsEqualToSomething() throws Exception {
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            pageRepository.saveAndFlush(page);
            space = SpaceResourceIT.createEntity();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        em.persist(space);
        em.flush();
        page.setSpace(space);
        pageRepository.saveAndFlush(page);
        Long spaceId = space.getId();
        // Get all the pageList where space equals to spaceId
        defaultPageShouldBeFound("spaceId.equals=" + spaceId);

        // Get all the pageList where space equals to (spaceId + 1)
        defaultPageShouldNotBeFound("spaceId.equals=" + (spaceId + 1));
    }

    @Test
    @Transactional
    void getAllPagesByParentIsEqualToSomething() throws Exception {
        // DTS-WIKI: customized (invariant I1 single root per space + I4 unique gitPath):
        // persist a dedicated root parent; the shared `page` fixture becomes its child.
        Page parent;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            parent = PageResourceIT.createEntity(em);
            parent.setGitPath("PARENT_GIT_PATH");
            em.persist(parent);
            em.flush();
        } else {
            parent = TestUtil.findAll(em, Page.class).get(0);
        }
        page.setSpace(parent.getSpace());
        page.setParent(parent);
        pageRepository.saveAndFlush(page);
        Long parentId = parent.getId();
        // Get all the pageList where parent equals to parentId
        defaultPageShouldBeFound("parentId.equals=" + parentId);

        // Get all the pageList where parent equals to (parentId + 1)
        defaultPageShouldNotBeFound("parentId.equals=" + (parentId + 1));
    }

    private void defaultPageFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPageShouldBeFound(shouldBeFound);
        defaultPageShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPageShouldBeFound(String filter) throws Exception {
        restPageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(page.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].gitPath").value(hasItem(DEFAULT_GIT_PATH)))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION)))
            .andExpect(jsonPath("$.[*].syncStatus").value(hasItem(DEFAULT_SYNC_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].deletedAt").value(hasItem(DEFAULT_DELETED_AT.toString())));

        // Check, that the count call also returns 1
        restPageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPageShouldNotBeFound(String filter) throws Exception {
        restPageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPage() throws Exception {
        // Get the page
        restPageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPage() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the page
        Page updatedPage = pageRepository.findById(page.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPage are not directly saved in db
        em.detach(updatedPage);
        updatedPage
            .title(UPDATED_TITLE)
            .kind(UPDATED_KIND)
            .gitPath(UPDATED_GIT_PATH)
            .position(UPDATED_POSITION)
            .syncStatus(UPDATED_SYNC_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .deletedAt(UPDATED_DELETED_AT);
        PageDTO pageDTO = pageMapper.toDto(updatedPage);

        restPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDTO))
            )
            .andExpect(status().isOk());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPageToMatchAllProperties(updatedPage);
    }

    @Test
    @Transactional
    void putNonExistingPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePageWithPatch() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the page using partial update
        Page partialUpdatedPage = new Page();
        partialUpdatedPage.setId(page.getId());

        partialUpdatedPage.gitPath(UPDATED_GIT_PATH).position(UPDATED_POSITION).createdAt(UPDATED_CREATED_AT).deletedAt(UPDATED_DELETED_AT);

        restPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPage.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPage))
            )
            .andExpect(status().isOk());

        // Validate the Page in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedPage, page), getPersistedPage(page));
    }

    @Test
    @Transactional
    void fullUpdatePageWithPatch() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the page using partial update
        Page partialUpdatedPage = new Page();
        partialUpdatedPage.setId(page.getId());

        partialUpdatedPage
            .title(UPDATED_TITLE)
            .kind(UPDATED_KIND)
            .gitPath(UPDATED_GIT_PATH)
            .position(UPDATED_POSITION)
            .syncStatus(UPDATED_SYNC_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .deletedAt(UPDATED_DELETED_AT);

        restPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPage.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPage))
            )
            .andExpect(status().isOk());

        // Validate the Page in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageUpdatableFieldsEquals(partialUpdatedPage, getPersistedPage(partialUpdatedPage));
    }

    @Test
    @Transactional
    void patchNonExistingPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, pageDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        page.setId(longCount.incrementAndGet());

        // Create the Page
        PageDTO pageDTO = pageMapper.toDto(page);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(pageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Page in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePage() throws Exception {
        // Initialize the database
        insertedPage = pageRepository.saveAndFlush(page);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the page
        restPageMockMvc
            .perform(delete(ENTITY_API_URL_ID, page.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return pageRepository.count();
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

    protected Page getPersistedPage(Page page) {
        return pageRepository.findById(page.getId()).orElseThrow();
    }

    protected void assertPersistedPageToMatchAllProperties(Page expectedPage) {
        assertPageAllPropertiesEquals(expectedPage, getPersistedPage(expectedPage));
    }

    protected void assertPersistedPageToMatchUpdatableProperties(Page expectedPage) {
        assertPageAllUpdatablePropertiesEquals(expectedPage, getPersistedPage(expectedPage));
    }
}
