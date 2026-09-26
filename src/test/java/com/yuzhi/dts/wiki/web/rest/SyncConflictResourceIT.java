package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.SyncConflictAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.SyncConflict;
import com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.service.SyncConflictService;
import com.yuzhi.dts.wiki.service.dto.SyncConflictDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncConflictMapper;
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
 * Integration tests for the {@link SyncConflictResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncConflictResourceIT {

    private static final String DEFAULT_GIT_CONTENT_MD = "AAAAAAAAAA";
    private static final String UPDATED_GIT_CONTENT_MD = "BBBBBBBBBB";

    private static final String DEFAULT_GIT_COMMIT = "AAAAAAAAAA";
    private static final String UPDATED_GIT_COMMIT = "BBBBBBBBBB";

    private static final Instant DEFAULT_DETECTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DETECTED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final Instant DEFAULT_RESOLVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESOLVED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final ConflictResolution DEFAULT_RESOLUTION = ConflictResolution.MERGED;
    private static final ConflictResolution UPDATED_RESOLUTION = ConflictResolution.KEPT_WIKI;

    private static final String ENTITY_API_URL = "/api/sync-conflicts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SyncConflictRepository syncConflictRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private SyncConflictRepository syncConflictRepositoryMock;

    @Autowired
    private SyncConflictMapper syncConflictMapper;

    @Mock
    private SyncConflictService syncConflictServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSyncConflictMockMvc;

    private SyncConflict syncConflict;

    private SyncConflict insertedSyncConflict;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncConflict createEntity(EntityManager em) {
        SyncConflict syncConflict = new SyncConflict()
            .gitContentMd(DEFAULT_GIT_CONTENT_MD)
            .gitCommit(DEFAULT_GIT_COMMIT)
            .detectedAt(DEFAULT_DETECTED_AT)
            .resolvedAt(DEFAULT_RESOLVED_AT)
            .resolution(DEFAULT_RESOLUTION);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        syncConflict.setPage(page);
        // Add required entity
        PageVersion pageVersion;
        if (TestUtil.findAll(em, PageVersion.class).isEmpty()) {
            pageVersion = PageVersionResourceIT.createEntity(em);
            em.persist(pageVersion);
            em.flush();
        } else {
            pageVersion = TestUtil.findAll(em, PageVersion.class).get(0);
        }
        syncConflict.setWikiVersion(pageVersion);
        return syncConflict;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncConflict createUpdatedEntity(EntityManager em) {
        SyncConflict updatedSyncConflict = new SyncConflict()
            .gitContentMd(UPDATED_GIT_CONTENT_MD)
            .gitCommit(UPDATED_GIT_COMMIT)
            .detectedAt(UPDATED_DETECTED_AT)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolution(UPDATED_RESOLUTION);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createUpdatedEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        updatedSyncConflict.setPage(page);
        // Add required entity
        PageVersion pageVersion;
        if (TestUtil.findAll(em, PageVersion.class).isEmpty()) {
            pageVersion = PageVersionResourceIT.createUpdatedEntity(em);
            em.persist(pageVersion);
            em.flush();
        } else {
            pageVersion = TestUtil.findAll(em, PageVersion.class).get(0);
        }
        updatedSyncConflict.setWikiVersion(pageVersion);
        return updatedSyncConflict;
    }

    @BeforeEach
    void initTest() {
        syncConflict = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSyncConflict != null) {
            syncConflictRepository.delete(insertedSyncConflict);
            insertedSyncConflict = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createSyncConflict() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);
        var returnedSyncConflictDTO = om.readValue(
            restSyncConflictMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncConflictDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SyncConflictDTO.class
        );

        // Validate the SyncConflict in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSyncConflict = syncConflictMapper.toEntity(returnedSyncConflictDTO);
        assertSyncConflictUpdatableFieldsEquals(returnedSyncConflict, getPersistedSyncConflict(returnedSyncConflict));

        insertedSyncConflict = returnedSyncConflict;
    }

    @Test
    @Transactional
    void createSyncConflictWithExistingId() throws Exception {
        // Create the SyncConflict with an existing ID
        syncConflict.setId(1L);
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSyncConflictMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkGitCommitIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncConflict.setGitCommit(null);

        // Create the SyncConflict, which fails.
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        restSyncConflictMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDetectedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncConflict.setDetectedAt(null);

        // Create the SyncConflict, which fails.
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        restSyncConflictMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSyncConflicts() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        // Get all the syncConflictList
        restSyncConflictMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(syncConflict.getId().intValue())))
            .andExpect(jsonPath("$.[*].gitContentMd").value(hasItem(DEFAULT_GIT_CONTENT_MD)))
            .andExpect(jsonPath("$.[*].gitCommit").value(hasItem(DEFAULT_GIT_COMMIT)))
            .andExpect(jsonPath("$.[*].detectedAt").value(hasItem(DEFAULT_DETECTED_AT.toString())))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())))
            .andExpect(jsonPath("$.[*].resolution").value(hasItem(DEFAULT_RESOLUTION.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncConflictsWithEagerRelationshipsIsEnabled() throws Exception {
        when(syncConflictServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncConflictMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(syncConflictServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncConflictsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(syncConflictServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncConflictMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(syncConflictRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSyncConflict() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        // Get the syncConflict
        restSyncConflictMockMvc
            .perform(get(ENTITY_API_URL_ID, syncConflict.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(syncConflict.getId().intValue()))
            .andExpect(jsonPath("$.gitContentMd").value(DEFAULT_GIT_CONTENT_MD))
            .andExpect(jsonPath("$.gitCommit").value(DEFAULT_GIT_COMMIT))
            .andExpect(jsonPath("$.detectedAt").value(DEFAULT_DETECTED_AT.toString()))
            .andExpect(jsonPath("$.resolvedAt").value(DEFAULT_RESOLVED_AT.toString()))
            .andExpect(jsonPath("$.resolution").value(DEFAULT_RESOLUTION.toString()));
    }

    @Test
    @Transactional
    void getNonExistingSyncConflict() throws Exception {
        // Get the syncConflict
        restSyncConflictMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSyncConflict() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncConflict
        SyncConflict updatedSyncConflict = syncConflictRepository.findById(syncConflict.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSyncConflict are not directly saved in db
        em.detach(updatedSyncConflict);
        updatedSyncConflict
            .gitContentMd(UPDATED_GIT_CONTENT_MD)
            .gitCommit(UPDATED_GIT_COMMIT)
            .detectedAt(UPDATED_DETECTED_AT)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolution(UPDATED_RESOLUTION);
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(updatedSyncConflict);

        restSyncConflictMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncConflictDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isOk());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSyncConflictToMatchAllProperties(updatedSyncConflict);
    }

    @Test
    @Transactional
    void putNonExistingSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncConflictDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSyncConflictWithPatch() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncConflict using partial update
        SyncConflict partialUpdatedSyncConflict = new SyncConflict();
        partialUpdatedSyncConflict.setId(syncConflict.getId());

        partialUpdatedSyncConflict.detectedAt(UPDATED_DETECTED_AT).resolvedAt(UPDATED_RESOLVED_AT).resolution(UPDATED_RESOLUTION);

        restSyncConflictMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncConflict.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncConflict))
            )
            .andExpect(status().isOk());

        // Validate the SyncConflict in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncConflictUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSyncConflict, syncConflict),
            getPersistedSyncConflict(syncConflict)
        );
    }

    @Test
    @Transactional
    void fullUpdateSyncConflictWithPatch() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncConflict using partial update
        SyncConflict partialUpdatedSyncConflict = new SyncConflict();
        partialUpdatedSyncConflict.setId(syncConflict.getId());

        partialUpdatedSyncConflict
            .gitContentMd(UPDATED_GIT_CONTENT_MD)
            .gitCommit(UPDATED_GIT_COMMIT)
            .detectedAt(UPDATED_DETECTED_AT)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolution(UPDATED_RESOLUTION);

        restSyncConflictMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncConflict.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncConflict))
            )
            .andExpect(status().isOk());

        // Validate the SyncConflict in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncConflictUpdatableFieldsEquals(partialUpdatedSyncConflict, getPersistedSyncConflict(partialUpdatedSyncConflict));
    }

    @Test
    @Transactional
    void patchNonExistingSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, syncConflictDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSyncConflict() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncConflict.setId(longCount.incrementAndGet());

        // Create the SyncConflict
        SyncConflictDTO syncConflictDTO = syncConflictMapper.toDto(syncConflict);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncConflictMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncConflictDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncConflict in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSyncConflict() throws Exception {
        // Initialize the database
        insertedSyncConflict = syncConflictRepository.saveAndFlush(syncConflict);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the syncConflict
        restSyncConflictMockMvc
            .perform(delete(ENTITY_API_URL_ID, syncConflict.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return syncConflictRepository.count();
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

    protected SyncConflict getPersistedSyncConflict(SyncConflict syncConflict) {
        return syncConflictRepository.findById(syncConflict.getId()).orElseThrow();
    }

    protected void assertPersistedSyncConflictToMatchAllProperties(SyncConflict expectedSyncConflict) {
        assertSyncConflictAllPropertiesEquals(expectedSyncConflict, getPersistedSyncConflict(expectedSyncConflict));
    }

    protected void assertPersistedSyncConflictToMatchUpdatableProperties(SyncConflict expectedSyncConflict) {
        assertSyncConflictAllUpdatablePropertiesEquals(expectedSyncConflict, getPersistedSyncConflict(expectedSyncConflict));
    }
}
