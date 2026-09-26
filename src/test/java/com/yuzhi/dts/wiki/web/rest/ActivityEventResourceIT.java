package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.ActivityEventAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.ActivityEvent;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.enumeration.ActivityType;
import com.yuzhi.dts.wiki.repository.ActivityEventRepository;
import com.yuzhi.dts.wiki.service.ActivityEventService;
import com.yuzhi.dts.wiki.service.mapper.ActivityEventMapper;
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
 * Integration tests for the {@link ActivityEventResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class ActivityEventResourceIT {

    private static final ActivityType DEFAULT_TYPE = ActivityType.PAGE_CREATED;
    private static final ActivityType UPDATED_TYPE = ActivityType.PAGE_UPDATED;

    private static final String DEFAULT_ACTOR_NAME = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_TARGET_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TARGET_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DETAIL = "AAAAAAAAAA";
    private static final String UPDATED_DETAIL = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/activity-events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Mock
    private ActivityEventRepository activityEventRepositoryMock;

    @Autowired
    private ActivityEventMapper activityEventMapper;

    @Mock
    private ActivityEventService activityEventServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restActivityEventMockMvc;

    private ActivityEvent activityEvent;

    private ActivityEvent insertedActivityEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ActivityEvent createEntity(EntityManager em) {
        ActivityEvent activityEvent = new ActivityEvent()
            .type(DEFAULT_TYPE)
            .actorName(DEFAULT_ACTOR_NAME)
            .targetTitle(DEFAULT_TARGET_TITLE)
            .detail(DEFAULT_DETAIL)
            .createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        activityEvent.setSpace(space);
        return activityEvent;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ActivityEvent createUpdatedEntity(EntityManager em) {
        ActivityEvent updatedActivityEvent = new ActivityEvent()
            .type(UPDATED_TYPE)
            .actorName(UPDATED_ACTOR_NAME)
            .targetTitle(UPDATED_TARGET_TITLE)
            .detail(UPDATED_DETAIL)
            .createdAt(UPDATED_CREATED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createUpdatedEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        updatedActivityEvent.setSpace(space);
        return updatedActivityEvent;
    }

    @BeforeEach
    void initTest() {
        activityEvent = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedActivityEvent != null) {
            activityEventRepository.delete(insertedActivityEvent);
            insertedActivityEvent = null;
        }
    }

    @Test
    @Transactional
    void getAllActivityEvents() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(activityEvent.getId().intValue())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].actorName").value(hasItem(DEFAULT_ACTOR_NAME)))
            .andExpect(jsonPath("$.[*].targetTitle").value(hasItem(DEFAULT_TARGET_TITLE)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllActivityEventsWithEagerRelationshipsIsEnabled() throws Exception {
        when(activityEventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restActivityEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(activityEventServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllActivityEventsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(activityEventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restActivityEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(activityEventRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getActivityEvent() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get the activityEvent
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL_ID, activityEvent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(activityEvent.getId().intValue()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.actorName").value(DEFAULT_ACTOR_NAME))
            .andExpect(jsonPath("$.targetTitle").value(DEFAULT_TARGET_TITLE))
            .andExpect(jsonPath("$.detail").value(DEFAULT_DETAIL))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getActivityEventsByIdFiltering() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        Long id = activityEvent.getId();

        defaultActivityEventFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultActivityEventFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultActivityEventFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where type equals to
        defaultActivityEventFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where type in
        defaultActivityEventFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where type is not null
        defaultActivityEventFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllActivityEventsByActorNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where actorName equals to
        defaultActivityEventFiltering("actorName.equals=" + DEFAULT_ACTOR_NAME, "actorName.equals=" + UPDATED_ACTOR_NAME);
    }

    @Test
    @Transactional
    void getAllActivityEventsByActorNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where actorName in
        defaultActivityEventFiltering(
            "actorName.in=" + DEFAULT_ACTOR_NAME + "," + UPDATED_ACTOR_NAME,
            "actorName.in=" + UPDATED_ACTOR_NAME
        );
    }

    @Test
    @Transactional
    void getAllActivityEventsByActorNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where actorName is not null
        defaultActivityEventFiltering("actorName.specified=true", "actorName.specified=false");
    }

    @Test
    @Transactional
    void getAllActivityEventsByActorNameContainsSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where actorName contains
        defaultActivityEventFiltering("actorName.contains=" + DEFAULT_ACTOR_NAME, "actorName.contains=" + UPDATED_ACTOR_NAME);
    }

    @Test
    @Transactional
    void getAllActivityEventsByActorNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where actorName does not contain
        defaultActivityEventFiltering("actorName.doesNotContain=" + UPDATED_ACTOR_NAME, "actorName.doesNotContain=" + DEFAULT_ACTOR_NAME);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTargetTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where targetTitle equals to
        defaultActivityEventFiltering("targetTitle.equals=" + DEFAULT_TARGET_TITLE, "targetTitle.equals=" + UPDATED_TARGET_TITLE);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTargetTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where targetTitle in
        defaultActivityEventFiltering(
            "targetTitle.in=" + DEFAULT_TARGET_TITLE + "," + UPDATED_TARGET_TITLE,
            "targetTitle.in=" + UPDATED_TARGET_TITLE
        );
    }

    @Test
    @Transactional
    void getAllActivityEventsByTargetTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where targetTitle is not null
        defaultActivityEventFiltering("targetTitle.specified=true", "targetTitle.specified=false");
    }

    @Test
    @Transactional
    void getAllActivityEventsByTargetTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where targetTitle contains
        defaultActivityEventFiltering("targetTitle.contains=" + DEFAULT_TARGET_TITLE, "targetTitle.contains=" + UPDATED_TARGET_TITLE);
    }

    @Test
    @Transactional
    void getAllActivityEventsByTargetTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where targetTitle does not contain
        defaultActivityEventFiltering(
            "targetTitle.doesNotContain=" + UPDATED_TARGET_TITLE,
            "targetTitle.doesNotContain=" + DEFAULT_TARGET_TITLE
        );
    }

    @Test
    @Transactional
    void getAllActivityEventsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where createdAt equals to
        defaultActivityEventFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllActivityEventsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where createdAt in
        defaultActivityEventFiltering(
            "createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT,
            "createdAt.in=" + UPDATED_CREATED_AT
        );
    }

    @Test
    @Transactional
    void getAllActivityEventsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActivityEvent = activityEventRepository.saveAndFlush(activityEvent);

        // Get all the activityEventList where createdAt is not null
        defaultActivityEventFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllActivityEventsBySpaceIsEqualToSomething() throws Exception {
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            activityEventRepository.saveAndFlush(activityEvent);
            space = SpaceResourceIT.createEntity();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        em.persist(space);
        em.flush();
        activityEvent.setSpace(space);
        activityEventRepository.saveAndFlush(activityEvent);
        Long spaceId = space.getId();
        // Get all the activityEventList where space equals to spaceId
        defaultActivityEventShouldBeFound("spaceId.equals=" + spaceId);

        // Get all the activityEventList where space equals to (spaceId + 1)
        defaultActivityEventShouldNotBeFound("spaceId.equals=" + (spaceId + 1));
    }

    @Test
    @Transactional
    void getAllActivityEventsByPageIsEqualToSomething() throws Exception {
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            activityEventRepository.saveAndFlush(activityEvent);
            page = PageResourceIT.createEntity(em);
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        em.persist(page);
        em.flush();
        activityEvent.setPage(page);
        activityEventRepository.saveAndFlush(activityEvent);
        Long pageId = page.getId();
        // Get all the activityEventList where page equals to pageId
        defaultActivityEventShouldBeFound("pageId.equals=" + pageId);

        // Get all the activityEventList where page equals to (pageId + 1)
        defaultActivityEventShouldNotBeFound("pageId.equals=" + (pageId + 1));
    }

    private void defaultActivityEventFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultActivityEventShouldBeFound(shouldBeFound);
        defaultActivityEventShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultActivityEventShouldBeFound(String filter) throws Exception {
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(activityEvent.getId().intValue())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].actorName").value(hasItem(DEFAULT_ACTOR_NAME)))
            .andExpect(jsonPath("$.[*].targetTitle").value(hasItem(DEFAULT_TARGET_TITLE)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));

        // Check, that the count call also returns 1
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultActivityEventShouldNotBeFound(String filter) throws Exception {
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restActivityEventMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingActivityEvent() throws Exception {
        // Get the activityEvent
        restActivityEventMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return activityEventRepository.count();
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

    protected ActivityEvent getPersistedActivityEvent(ActivityEvent activityEvent) {
        return activityEventRepository.findById(activityEvent.getId()).orElseThrow();
    }

    protected void assertPersistedActivityEventToMatchAllProperties(ActivityEvent expectedActivityEvent) {
        assertActivityEventAllPropertiesEquals(expectedActivityEvent, getPersistedActivityEvent(expectedActivityEvent));
    }

    protected void assertPersistedActivityEventToMatchUpdatableProperties(ActivityEvent expectedActivityEvent) {
        assertActivityEventAllUpdatablePropertiesEquals(expectedActivityEvent, getPersistedActivityEvent(expectedActivityEvent));
    }
}
