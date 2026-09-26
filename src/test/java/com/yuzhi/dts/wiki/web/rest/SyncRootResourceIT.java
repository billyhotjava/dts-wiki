package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.SyncRootAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.service.SyncRootService;
import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncRootMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link SyncRootResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncRootResourceIT {

    private static final String DEFAULT_REPO_PATH = "AAAAAAAAAA";
    private static final String UPDATED_REPO_PATH = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    private static final String ENTITY_API_URL = "/api/sync-roots";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SyncRootRepository syncRootRepository;

    @Mock
    private SyncRootRepository syncRootRepositoryMock;

    @Autowired
    private SyncRootMapper syncRootMapper;

    @Mock
    private SyncRootService syncRootServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSyncRootMockMvc;

    private SyncRoot syncRoot;

    private SyncRoot insertedSyncRoot;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncRoot createEntity(EntityManager em) {
        SyncRoot syncRoot = new SyncRoot().repoPath(DEFAULT_REPO_PATH).enabled(DEFAULT_ENABLED);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        syncRoot.setSpace(space);
        return syncRoot;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncRoot createUpdatedEntity(EntityManager em) {
        SyncRoot updatedSyncRoot = new SyncRoot().repoPath(UPDATED_REPO_PATH).enabled(UPDATED_ENABLED);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createUpdatedEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        updatedSyncRoot.setSpace(space);
        return updatedSyncRoot;
    }

    @BeforeEach
    void initTest() {
        syncRoot = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSyncRoot != null) {
            syncRootRepository.delete(insertedSyncRoot);
            insertedSyncRoot = null;
        }
    }

    @Test
    @Transactional
    void createSyncRoot() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);
        var returnedSyncRootDTO = om.readValue(
            restSyncRootMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncRootDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SyncRootDTO.class
        );

        // Validate the SyncRoot in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSyncRoot = syncRootMapper.toEntity(returnedSyncRootDTO);
        assertSyncRootUpdatableFieldsEquals(returnedSyncRoot, getPersistedSyncRoot(returnedSyncRoot));

        insertedSyncRoot = returnedSyncRoot;
    }

    @Test
    @Transactional
    void createSyncRootWithExistingId() throws Exception {
        // Create the SyncRoot with an existing ID
        syncRoot.setId(1L);
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSyncRootMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncRootDTO)))
            .andExpect(status().isBadRequest());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRepoPathIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncRoot.setRepoPath(null);

        // Create the SyncRoot, which fails.
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        restSyncRootMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncRootDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncRoot.setEnabled(null);

        // Create the SyncRoot, which fails.
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        restSyncRootMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncRootDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSyncRoots() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        // Get all the syncRootList
        restSyncRootMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(syncRoot.getId().intValue())))
            .andExpect(jsonPath("$.[*].repoPath").value(hasItem(DEFAULT_REPO_PATH)))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncRootsWithEagerRelationshipsIsEnabled() throws Exception {
        when(syncRootServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncRootMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(syncRootServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncRootsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(syncRootServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncRootMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(syncRootRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSyncRoot() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        // Get the syncRoot
        restSyncRootMockMvc
            .perform(get(ENTITY_API_URL_ID, syncRoot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(syncRoot.getId().intValue()))
            .andExpect(jsonPath("$.repoPath").value(DEFAULT_REPO_PATH))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED));
    }

    @Test
    @Transactional
    void getNonExistingSyncRoot() throws Exception {
        // Get the syncRoot
        restSyncRootMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSyncRoot() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncRoot
        SyncRoot updatedSyncRoot = syncRootRepository.findById(syncRoot.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSyncRoot are not directly saved in db
        em.detach(updatedSyncRoot);
        updatedSyncRoot.repoPath(UPDATED_REPO_PATH).enabled(UPDATED_ENABLED);
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(updatedSyncRoot);

        restSyncRootMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncRootDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isOk());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSyncRootToMatchAllProperties(updatedSyncRoot);
    }

    @Test
    @Transactional
    void putNonExistingSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncRootDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncRootDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSyncRootWithPatch() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncRoot using partial update
        SyncRoot partialUpdatedSyncRoot = new SyncRoot();
        partialUpdatedSyncRoot.setId(syncRoot.getId());

        partialUpdatedSyncRoot.repoPath(UPDATED_REPO_PATH).enabled(UPDATED_ENABLED);

        restSyncRootMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncRoot.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncRoot))
            )
            .andExpect(status().isOk());

        // Validate the SyncRoot in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncRootUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedSyncRoot, syncRoot), getPersistedSyncRoot(syncRoot));
    }

    @Test
    @Transactional
    void fullUpdateSyncRootWithPatch() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncRoot using partial update
        SyncRoot partialUpdatedSyncRoot = new SyncRoot();
        partialUpdatedSyncRoot.setId(syncRoot.getId());

        partialUpdatedSyncRoot.repoPath(UPDATED_REPO_PATH).enabled(UPDATED_ENABLED);

        restSyncRootMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncRoot.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncRoot))
            )
            .andExpect(status().isOk());

        // Validate the SyncRoot in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncRootUpdatableFieldsEquals(partialUpdatedSyncRoot, getPersistedSyncRoot(partialUpdatedSyncRoot));
    }

    @Test
    @Transactional
    void patchNonExistingSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, syncRootDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSyncRoot() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncRoot.setId(longCount.incrementAndGet());

        // Create the SyncRoot
        SyncRootDTO syncRootDTO = syncRootMapper.toDto(syncRoot);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncRootMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(syncRootDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncRoot in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSyncRoot() throws Exception {
        // Initialize the database
        insertedSyncRoot = syncRootRepository.saveAndFlush(syncRoot);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the syncRoot
        restSyncRootMockMvc
            .perform(delete(ENTITY_API_URL_ID, syncRoot.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return syncRootRepository.count();
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

    protected SyncRoot getPersistedSyncRoot(SyncRoot syncRoot) {
        return syncRootRepository.findById(syncRoot.getId()).orElseThrow();
    }

    protected void assertPersistedSyncRootToMatchAllProperties(SyncRoot expectedSyncRoot) {
        assertSyncRootAllPropertiesEquals(expectedSyncRoot, getPersistedSyncRoot(expectedSyncRoot));
    }

    protected void assertPersistedSyncRootToMatchUpdatableProperties(SyncRoot expectedSyncRoot) {
        assertSyncRootAllUpdatablePropertiesEquals(expectedSyncRoot, getPersistedSyncRoot(expectedSyncRoot));
    }
}
