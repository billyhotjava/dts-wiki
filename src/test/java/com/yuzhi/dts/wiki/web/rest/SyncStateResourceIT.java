package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.SyncStateAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.SyncState;
import com.yuzhi.dts.wiki.domain.enumeration.SyncRunStatus;
import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import com.yuzhi.dts.wiki.service.dto.SyncStateDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncStateMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SyncStateResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncStateResourceIT {

    private static final String DEFAULT_LAST_SYNCED_COMMIT = "AAAAAAAAAA";
    private static final String UPDATED_LAST_SYNCED_COMMIT = "BBBBBBBBBB";

    private static final Instant DEFAULT_LAST_FETCH_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_FETCH_AT = Instant.ofEpochMilli(1702275806009L);

    private static final Instant DEFAULT_LAST_PUSH_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_PUSH_AT = Instant.ofEpochMilli(1702275806009L);

    private static final SyncRunStatus DEFAULT_STATUS = SyncRunStatus.IDLE;
    private static final SyncRunStatus UPDATED_STATUS = SyncRunStatus.RUNNING;

    private static final String DEFAULT_MESSAGE = "AAAAAAAAAA";
    private static final String UPDATED_MESSAGE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/sync-states";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SyncStateRepository syncStateRepository;

    @Autowired
    private SyncStateMapper syncStateMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSyncStateMockMvc;

    private SyncState syncState;

    private SyncState insertedSyncState;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncState createEntity(EntityManager em) {
        SyncState syncState = new SyncState()
            .lastSyncedCommit(DEFAULT_LAST_SYNCED_COMMIT)
            .lastFetchAt(DEFAULT_LAST_FETCH_AT)
            .lastPushAt(DEFAULT_LAST_PUSH_AT)
            .status(DEFAULT_STATUS)
            .message(DEFAULT_MESSAGE);
        // Add required entity
        SyncRoot syncRoot;
        if (TestUtil.findAll(em, SyncRoot.class).isEmpty()) {
            syncRoot = SyncRootResourceIT.createEntity(em);
            em.persist(syncRoot);
            em.flush();
        } else {
            syncRoot = TestUtil.findAll(em, SyncRoot.class).get(0);
        }
        syncState.setSyncRoot(syncRoot);
        return syncState;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncState createUpdatedEntity(EntityManager em) {
        SyncState updatedSyncState = new SyncState()
            .lastSyncedCommit(UPDATED_LAST_SYNCED_COMMIT)
            .lastFetchAt(UPDATED_LAST_FETCH_AT)
            .lastPushAt(UPDATED_LAST_PUSH_AT)
            .status(UPDATED_STATUS)
            .message(UPDATED_MESSAGE);
        // Add required entity
        SyncRoot syncRoot;
        if (TestUtil.findAll(em, SyncRoot.class).isEmpty()) {
            syncRoot = SyncRootResourceIT.createUpdatedEntity(em);
            em.persist(syncRoot);
            em.flush();
        } else {
            syncRoot = TestUtil.findAll(em, SyncRoot.class).get(0);
        }
        updatedSyncState.setSyncRoot(syncRoot);
        return updatedSyncState;
    }

    @BeforeEach
    void initTest() {
        syncState = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSyncState != null) {
            syncStateRepository.delete(insertedSyncState);
            insertedSyncState = null;
        }
    }

    @Test
    @Transactional
    void createSyncState() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);
        var returnedSyncStateDTO = om.readValue(
            restSyncStateMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncStateDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SyncStateDTO.class
        );

        // Validate the SyncState in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSyncState = syncStateMapper.toEntity(returnedSyncStateDTO);
        assertSyncStateUpdatableFieldsEquals(returnedSyncState, getPersistedSyncState(returnedSyncState));

        insertedSyncState = returnedSyncState;
    }

    @Test
    @Transactional
    void createSyncStateWithExistingId() throws Exception {
        // Create the SyncState with an existing ID
        syncState.setId(1L);
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSyncStateMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncStateDTO)))
            .andExpect(status().isBadRequest());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncState.setStatus(null);

        // Create the SyncState, which fails.
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        restSyncStateMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncStateDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSyncStates() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        // Get all the syncStateList
        restSyncStateMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(syncState.getId().intValue())))
            .andExpect(jsonPath("$.[*].lastSyncedCommit").value(hasItem(DEFAULT_LAST_SYNCED_COMMIT)))
            .andExpect(jsonPath("$.[*].lastFetchAt").value(hasItem(DEFAULT_LAST_FETCH_AT.toString())))
            .andExpect(jsonPath("$.[*].lastPushAt").value(hasItem(DEFAULT_LAST_PUSH_AT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE)));
    }

    @Test
    @Transactional
    void getSyncState() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        // Get the syncState
        restSyncStateMockMvc
            .perform(get(ENTITY_API_URL_ID, syncState.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(syncState.getId().intValue()))
            .andExpect(jsonPath("$.lastSyncedCommit").value(DEFAULT_LAST_SYNCED_COMMIT))
            .andExpect(jsonPath("$.lastFetchAt").value(DEFAULT_LAST_FETCH_AT.toString()))
            .andExpect(jsonPath("$.lastPushAt").value(DEFAULT_LAST_PUSH_AT.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.message").value(DEFAULT_MESSAGE));
    }

    @Test
    @Transactional
    void getNonExistingSyncState() throws Exception {
        // Get the syncState
        restSyncStateMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSyncState() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncState
        SyncState updatedSyncState = syncStateRepository.findById(syncState.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSyncState are not directly saved in db
        em.detach(updatedSyncState);
        updatedSyncState
            .lastSyncedCommit(UPDATED_LAST_SYNCED_COMMIT)
            .lastFetchAt(UPDATED_LAST_FETCH_AT)
            .lastPushAt(UPDATED_LAST_PUSH_AT)
            .status(UPDATED_STATUS)
            .message(UPDATED_MESSAGE);
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(updatedSyncState);

        restSyncStateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncStateDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isOk());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSyncStateToMatchAllProperties(updatedSyncState);
    }

    @Test
    @Transactional
    void putNonExistingSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncStateDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncStateDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSyncStateWithPatch() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncState using partial update
        SyncState partialUpdatedSyncState = new SyncState();
        partialUpdatedSyncState.setId(syncState.getId());

        partialUpdatedSyncState.lastPushAt(UPDATED_LAST_PUSH_AT).status(UPDATED_STATUS).message(UPDATED_MESSAGE);

        restSyncStateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncState.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncState))
            )
            .andExpect(status().isOk());

        // Validate the SyncState in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncStateUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSyncState, syncState),
            getPersistedSyncState(syncState)
        );
    }

    @Test
    @Transactional
    void fullUpdateSyncStateWithPatch() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncState using partial update
        SyncState partialUpdatedSyncState = new SyncState();
        partialUpdatedSyncState.setId(syncState.getId());

        partialUpdatedSyncState
            .lastSyncedCommit(UPDATED_LAST_SYNCED_COMMIT)
            .lastFetchAt(UPDATED_LAST_FETCH_AT)
            .lastPushAt(UPDATED_LAST_PUSH_AT)
            .status(UPDATED_STATUS)
            .message(UPDATED_MESSAGE);

        restSyncStateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncState.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncState))
            )
            .andExpect(status().isOk());

        // Validate the SyncState in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncStateUpdatableFieldsEquals(partialUpdatedSyncState, getPersistedSyncState(partialUpdatedSyncState));
    }

    @Test
    @Transactional
    void patchNonExistingSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, syncStateDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSyncState() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncState.setId(longCount.incrementAndGet());

        // Create the SyncState
        SyncStateDTO syncStateDTO = syncStateMapper.toDto(syncState);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncStateMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(syncStateDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncState in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSyncState() throws Exception {
        // Initialize the database
        insertedSyncState = syncStateRepository.saveAndFlush(syncState);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the syncState
        restSyncStateMockMvc
            .perform(delete(ENTITY_API_URL_ID, syncState.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return syncStateRepository.count();
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

    protected SyncState getPersistedSyncState(SyncState syncState) {
        return syncStateRepository.findById(syncState.getId()).orElseThrow();
    }

    protected void assertPersistedSyncStateToMatchAllProperties(SyncState expectedSyncState) {
        assertSyncStateAllPropertiesEquals(expectedSyncState, getPersistedSyncState(expectedSyncState));
    }

    protected void assertPersistedSyncStateToMatchUpdatableProperties(SyncState expectedSyncState) {
        assertSyncStateAllUpdatablePropertiesEquals(expectedSyncState, getPersistedSyncState(expectedSyncState));
    }
}
