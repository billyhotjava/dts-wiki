package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.PageDraftAsserts.*;
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
import com.yuzhi.dts.wiki.domain.PageDraft;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.PageDraftRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.service.PageDraftService;
import com.yuzhi.dts.wiki.service.dto.PageDraftDTO;
import com.yuzhi.dts.wiki.service.mapper.PageDraftMapper;
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
 * Integration tests for the {@link PageDraftResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class PageDraftResourceIT {

    private static final String DEFAULT_CONTENT_MD = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT_MD = "BBBBBBBBBB";

    private static final Integer DEFAULT_BASE_VERSION_NO = 1;
    private static final Integer UPDATED_BASE_VERSION_NO = 2;

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/page-drafts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PageDraftRepository pageDraftRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private PageDraftRepository pageDraftRepositoryMock;

    @Autowired
    private PageDraftMapper pageDraftMapper;

    @Mock
    private PageDraftService pageDraftServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPageDraftMockMvc;

    private PageDraft pageDraft;

    private PageDraft insertedPageDraft;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageDraft createEntity(EntityManager em) {
        PageDraft pageDraft = new PageDraft()
            .contentMd(DEFAULT_CONTENT_MD)
            .baseVersionNo(DEFAULT_BASE_VERSION_NO)
            .updatedAt(DEFAULT_UPDATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        pageDraft.setPage(page);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        pageDraft.setUser(user);
        return pageDraft;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageDraft createUpdatedEntity(EntityManager em) {
        PageDraft updatedPageDraft = new PageDraft()
            .contentMd(UPDATED_CONTENT_MD)
            .baseVersionNo(UPDATED_BASE_VERSION_NO)
            .updatedAt(UPDATED_UPDATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createUpdatedEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        updatedPageDraft.setPage(page);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        updatedPageDraft.setUser(user);
        return updatedPageDraft;
    }

    @BeforeEach
    void initTest() {
        pageDraft = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPageDraft != null) {
            pageDraftRepository.delete(insertedPageDraft);
            insertedPageDraft = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createPageDraft() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);
        var returnedPageDraftDTO = om.readValue(
            restPageDraftMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDraftDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PageDraftDTO.class
        );

        // Validate the PageDraft in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPageDraft = pageDraftMapper.toEntity(returnedPageDraftDTO);
        assertPageDraftUpdatableFieldsEquals(returnedPageDraft, getPersistedPageDraft(returnedPageDraft));

        insertedPageDraft = returnedPageDraft;
    }

    @Test
    @Transactional
    void createPageDraftWithExistingId() throws Exception {
        // Create the PageDraft with an existing ID
        pageDraft.setId(1L);
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPageDraftMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDraftDTO)))
            .andExpect(status().isBadRequest());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkBaseVersionNoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pageDraft.setBaseVersionNo(null);

        // Create the PageDraft, which fails.
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        restPageDraftMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDraftDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUpdatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pageDraft.setUpdatedAt(null);

        // Create the PageDraft, which fails.
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        restPageDraftMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDraftDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPageDrafts() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        // Get all the pageDraftList
        restPageDraftMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pageDraft.getId().intValue())))
            .andExpect(jsonPath("$.[*].contentMd").value(hasItem(DEFAULT_CONTENT_MD)))
            .andExpect(jsonPath("$.[*].baseVersionNo").value(hasItem(DEFAULT_BASE_VERSION_NO)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageDraftsWithEagerRelationshipsIsEnabled() throws Exception {
        when(pageDraftServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageDraftMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pageDraftServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageDraftsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pageDraftServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageDraftMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pageDraftRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPageDraft() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        // Get the pageDraft
        restPageDraftMockMvc
            .perform(get(ENTITY_API_URL_ID, pageDraft.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pageDraft.getId().intValue()))
            .andExpect(jsonPath("$.contentMd").value(DEFAULT_CONTENT_MD))
            .andExpect(jsonPath("$.baseVersionNo").value(DEFAULT_BASE_VERSION_NO))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingPageDraft() throws Exception {
        // Get the pageDraft
        restPageDraftMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPageDraft() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageDraft
        PageDraft updatedPageDraft = pageDraftRepository.findById(pageDraft.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPageDraft are not directly saved in db
        em.detach(updatedPageDraft);
        updatedPageDraft.contentMd(UPDATED_CONTENT_MD).baseVersionNo(UPDATED_BASE_VERSION_NO).updatedAt(UPDATED_UPDATED_AT);
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(updatedPageDraft);

        restPageDraftMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageDraftDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isOk());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPageDraftToMatchAllProperties(updatedPageDraft);
    }

    @Test
    @Transactional
    void putNonExistingPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageDraftDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageDraftDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePageDraftWithPatch() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageDraft using partial update
        PageDraft partialUpdatedPageDraft = new PageDraft();
        partialUpdatedPageDraft.setId(pageDraft.getId());

        partialUpdatedPageDraft.contentMd(UPDATED_CONTENT_MD).baseVersionNo(UPDATED_BASE_VERSION_NO);

        restPageDraftMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPageDraft.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPageDraft))
            )
            .andExpect(status().isOk());

        // Validate the PageDraft in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageDraftUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPageDraft, pageDraft),
            getPersistedPageDraft(pageDraft)
        );
    }

    @Test
    @Transactional
    void fullUpdatePageDraftWithPatch() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageDraft using partial update
        PageDraft partialUpdatedPageDraft = new PageDraft();
        partialUpdatedPageDraft.setId(pageDraft.getId());

        partialUpdatedPageDraft.contentMd(UPDATED_CONTENT_MD).baseVersionNo(UPDATED_BASE_VERSION_NO).updatedAt(UPDATED_UPDATED_AT);

        restPageDraftMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPageDraft.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPageDraft))
            )
            .andExpect(status().isOk());

        // Validate the PageDraft in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageDraftUpdatableFieldsEquals(partialUpdatedPageDraft, getPersistedPageDraft(partialUpdatedPageDraft));
    }

    @Test
    @Transactional
    void patchNonExistingPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, pageDraftDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPageDraft() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageDraft.setId(longCount.incrementAndGet());

        // Create the PageDraft
        PageDraftDTO pageDraftDTO = pageDraftMapper.toDto(pageDraft);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageDraftMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(pageDraftDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PageDraft in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePageDraft() throws Exception {
        // Initialize the database
        insertedPageDraft = pageDraftRepository.saveAndFlush(pageDraft);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the pageDraft
        restPageDraftMockMvc
            .perform(delete(ENTITY_API_URL_ID, pageDraft.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return pageDraftRepository.count();
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

    protected PageDraft getPersistedPageDraft(PageDraft pageDraft) {
        return pageDraftRepository.findById(pageDraft.getId()).orElseThrow();
    }

    protected void assertPersistedPageDraftToMatchAllProperties(PageDraft expectedPageDraft) {
        assertPageDraftAllPropertiesEquals(expectedPageDraft, getPersistedPageDraft(expectedPageDraft));
    }

    protected void assertPersistedPageDraftToMatchUpdatableProperties(PageDraft expectedPageDraft) {
        assertPageDraftAllUpdatablePropertiesEquals(expectedPageDraft, getPersistedPageDraft(expectedPageDraft));
    }
}
