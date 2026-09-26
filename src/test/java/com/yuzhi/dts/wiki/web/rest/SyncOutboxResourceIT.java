package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.SyncOutboxAsserts.*;
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
import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.SyncOutboxService;
import com.yuzhi.dts.wiki.service.dto.SyncOutboxDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncOutboxMapper;
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
 * Integration tests for the {@link SyncOutboxResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncOutboxResourceIT {

    private static final OutboxOp DEFAULT_OP = OutboxOp.WRITE;
    private static final OutboxOp UPDATED_OP = OutboxOp.MOVE;

    private static final String DEFAULT_PAYLOAD = "AAAAAAAAAA";
    private static final String UPDATED_PAYLOAD = "BBBBBBBBBB";

    private static final String DEFAULT_ACTOR_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_LOGIN = "BBBBBBBBBB";

    private static final String DEFAULT_ACTOR_NAME = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_ACTOR_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_EMAIL = "BBBBBBBBBB";

    private static final OutboxStatus DEFAULT_STATUS = OutboxStatus.PENDING;
    private static final OutboxStatus UPDATED_STATUS = OutboxStatus.DONE;

    private static final Integer DEFAULT_ATTEMPTS = 1;
    private static final Integer UPDATED_ATTEMPTS = 2;

    private static final String DEFAULT_LAST_ERROR = "AAAAAAAAAA";
    private static final String UPDATED_LAST_ERROR = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final Instant DEFAULT_PROCESSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PROCESSED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/sync-outboxes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SyncOutboxRepository syncOutboxRepository;

    @Mock
    private SyncOutboxRepository syncOutboxRepositoryMock;

    @Autowired
    private SyncOutboxMapper syncOutboxMapper;

    @Mock
    private SyncOutboxService syncOutboxServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSyncOutboxMockMvc;

    private SyncOutbox syncOutbox;

    private SyncOutbox insertedSyncOutbox;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncOutbox createEntity(EntityManager em) {
        SyncOutbox syncOutbox = new SyncOutbox()
            .op(DEFAULT_OP)
            .payload(DEFAULT_PAYLOAD)
            .actorLogin(DEFAULT_ACTOR_LOGIN)
            .actorName(DEFAULT_ACTOR_NAME)
            .actorEmail(DEFAULT_ACTOR_EMAIL)
            .status(DEFAULT_STATUS)
            .attempts(DEFAULT_ATTEMPTS)
            .lastError(DEFAULT_LAST_ERROR)
            .createdAt(DEFAULT_CREATED_AT)
            .processedAt(DEFAULT_PROCESSED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        syncOutbox.setSpace(space);
        return syncOutbox;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SyncOutbox createUpdatedEntity(EntityManager em) {
        SyncOutbox updatedSyncOutbox = new SyncOutbox()
            .op(UPDATED_OP)
            .payload(UPDATED_PAYLOAD)
            .actorLogin(UPDATED_ACTOR_LOGIN)
            .actorName(UPDATED_ACTOR_NAME)
            .actorEmail(UPDATED_ACTOR_EMAIL)
            .status(UPDATED_STATUS)
            .attempts(UPDATED_ATTEMPTS)
            .lastError(UPDATED_LAST_ERROR)
            .createdAt(UPDATED_CREATED_AT)
            .processedAt(UPDATED_PROCESSED_AT);
        // Add required entity
        Space space;
        if (TestUtil.findAll(em, Space.class).isEmpty()) {
            space = SpaceResourceIT.createUpdatedEntity();
            em.persist(space);
            em.flush();
        } else {
            space = TestUtil.findAll(em, Space.class).get(0);
        }
        updatedSyncOutbox.setSpace(space);
        return updatedSyncOutbox;
    }

    @BeforeEach
    void initTest() {
        syncOutbox = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSyncOutbox != null) {
            syncOutboxRepository.delete(insertedSyncOutbox);
            insertedSyncOutbox = null;
        }
    }

    @Test
    @Transactional
    void createSyncOutbox() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);
        var returnedSyncOutboxDTO = om.readValue(
            restSyncOutboxMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SyncOutboxDTO.class
        );

        // Validate the SyncOutbox in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSyncOutbox = syncOutboxMapper.toEntity(returnedSyncOutboxDTO);
        assertSyncOutboxUpdatableFieldsEquals(returnedSyncOutbox, getPersistedSyncOutbox(returnedSyncOutbox));

        insertedSyncOutbox = returnedSyncOutbox;
    }

    @Test
    @Transactional
    void createSyncOutboxWithExistingId() throws Exception {
        // Create the SyncOutbox with an existing ID
        syncOutbox.setId(1L);
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOpIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncOutbox.setOp(null);

        // Create the SyncOutbox, which fails.
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActorLoginIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncOutbox.setActorLogin(null);

        // Create the SyncOutbox, which fails.
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncOutbox.setStatus(null);

        // Create the SyncOutbox, which fails.
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAttemptsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncOutbox.setAttempts(null);

        // Create the SyncOutbox, which fails.
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        syncOutbox.setCreatedAt(null);

        // Create the SyncOutbox, which fails.
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        restSyncOutboxMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSyncOutboxes() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        // Get all the syncOutboxList
        restSyncOutboxMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(syncOutbox.getId().intValue())))
            .andExpect(jsonPath("$.[*].op").value(hasItem(DEFAULT_OP.toString())))
            .andExpect(jsonPath("$.[*].payload").value(hasItem(DEFAULT_PAYLOAD)))
            .andExpect(jsonPath("$.[*].actorLogin").value(hasItem(DEFAULT_ACTOR_LOGIN)))
            .andExpect(jsonPath("$.[*].actorName").value(hasItem(DEFAULT_ACTOR_NAME)))
            .andExpect(jsonPath("$.[*].actorEmail").value(hasItem(DEFAULT_ACTOR_EMAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].attempts").value(hasItem(DEFAULT_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].lastError").value(hasItem(DEFAULT_LAST_ERROR)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].processedAt").value(hasItem(DEFAULT_PROCESSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncOutboxesWithEagerRelationshipsIsEnabled() throws Exception {
        when(syncOutboxServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncOutboxMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(syncOutboxServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSyncOutboxesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(syncOutboxServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSyncOutboxMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(syncOutboxRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSyncOutbox() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        // Get the syncOutbox
        restSyncOutboxMockMvc
            .perform(get(ENTITY_API_URL_ID, syncOutbox.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(syncOutbox.getId().intValue()))
            .andExpect(jsonPath("$.op").value(DEFAULT_OP.toString()))
            .andExpect(jsonPath("$.payload").value(DEFAULT_PAYLOAD))
            .andExpect(jsonPath("$.actorLogin").value(DEFAULT_ACTOR_LOGIN))
            .andExpect(jsonPath("$.actorName").value(DEFAULT_ACTOR_NAME))
            .andExpect(jsonPath("$.actorEmail").value(DEFAULT_ACTOR_EMAIL))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.attempts").value(DEFAULT_ATTEMPTS))
            .andExpect(jsonPath("$.lastError").value(DEFAULT_LAST_ERROR))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.processedAt").value(DEFAULT_PROCESSED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingSyncOutbox() throws Exception {
        // Get the syncOutbox
        restSyncOutboxMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSyncOutbox() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncOutbox
        SyncOutbox updatedSyncOutbox = syncOutboxRepository.findById(syncOutbox.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSyncOutbox are not directly saved in db
        em.detach(updatedSyncOutbox);
        updatedSyncOutbox
            .op(UPDATED_OP)
            .payload(UPDATED_PAYLOAD)
            .actorLogin(UPDATED_ACTOR_LOGIN)
            .actorName(UPDATED_ACTOR_NAME)
            .actorEmail(UPDATED_ACTOR_EMAIL)
            .status(UPDATED_STATUS)
            .attempts(UPDATED_ATTEMPTS)
            .lastError(UPDATED_LAST_ERROR)
            .createdAt(UPDATED_CREATED_AT)
            .processedAt(UPDATED_PROCESSED_AT);
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(updatedSyncOutbox);

        restSyncOutboxMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncOutboxDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isOk());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSyncOutboxToMatchAllProperties(updatedSyncOutbox);
    }

    @Test
    @Transactional
    void putNonExistingSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(
                put(ENTITY_API_URL_ID, syncOutboxDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(syncOutboxDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSyncOutboxWithPatch() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncOutbox using partial update
        SyncOutbox partialUpdatedSyncOutbox = new SyncOutbox();
        partialUpdatedSyncOutbox.setId(syncOutbox.getId());

        partialUpdatedSyncOutbox
            .op(UPDATED_OP)
            .payload(UPDATED_PAYLOAD)
            .actorName(UPDATED_ACTOR_NAME)
            .status(UPDATED_STATUS)
            .attempts(UPDATED_ATTEMPTS)
            .createdAt(UPDATED_CREATED_AT);

        restSyncOutboxMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncOutbox.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncOutbox))
            )
            .andExpect(status().isOk());

        // Validate the SyncOutbox in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncOutboxUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSyncOutbox, syncOutbox),
            getPersistedSyncOutbox(syncOutbox)
        );
    }

    @Test
    @Transactional
    void fullUpdateSyncOutboxWithPatch() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the syncOutbox using partial update
        SyncOutbox partialUpdatedSyncOutbox = new SyncOutbox();
        partialUpdatedSyncOutbox.setId(syncOutbox.getId());

        partialUpdatedSyncOutbox
            .op(UPDATED_OP)
            .payload(UPDATED_PAYLOAD)
            .actorLogin(UPDATED_ACTOR_LOGIN)
            .actorName(UPDATED_ACTOR_NAME)
            .actorEmail(UPDATED_ACTOR_EMAIL)
            .status(UPDATED_STATUS)
            .attempts(UPDATED_ATTEMPTS)
            .lastError(UPDATED_LAST_ERROR)
            .createdAt(UPDATED_CREATED_AT)
            .processedAt(UPDATED_PROCESSED_AT);

        restSyncOutboxMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSyncOutbox.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSyncOutbox))
            )
            .andExpect(status().isOk());

        // Validate the SyncOutbox in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSyncOutboxUpdatableFieldsEquals(partialUpdatedSyncOutbox, getPersistedSyncOutbox(partialUpdatedSyncOutbox));
    }

    @Test
    @Transactional
    void patchNonExistingSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, syncOutboxDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSyncOutbox() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        syncOutbox.setId(longCount.incrementAndGet());

        // Create the SyncOutbox
        SyncOutboxDTO syncOutboxDTO = syncOutboxMapper.toDto(syncOutbox);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSyncOutboxMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(syncOutboxDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SyncOutbox in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSyncOutbox() throws Exception {
        // Initialize the database
        insertedSyncOutbox = syncOutboxRepository.saveAndFlush(syncOutbox);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the syncOutbox
        restSyncOutboxMockMvc
            .perform(delete(ENTITY_API_URL_ID, syncOutbox.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return syncOutboxRepository.count();
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

    protected SyncOutbox getPersistedSyncOutbox(SyncOutbox syncOutbox) {
        return syncOutboxRepository.findById(syncOutbox.getId()).orElseThrow();
    }

    protected void assertPersistedSyncOutboxToMatchAllProperties(SyncOutbox expectedSyncOutbox) {
        assertSyncOutboxAllPropertiesEquals(expectedSyncOutbox, getPersistedSyncOutbox(expectedSyncOutbox));
    }

    protected void assertPersistedSyncOutboxToMatchUpdatableProperties(SyncOutbox expectedSyncOutbox) {
        assertSyncOutboxAllUpdatablePropertiesEquals(expectedSyncOutbox, getPersistedSyncOutbox(expectedSyncOutbox));
    }
}
