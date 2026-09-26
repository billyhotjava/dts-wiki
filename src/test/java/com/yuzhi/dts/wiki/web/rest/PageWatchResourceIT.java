package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.PageWatchAsserts.*;
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
import com.yuzhi.dts.wiki.domain.PageWatch;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.PageWatchRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.service.PageWatchService;
import com.yuzhi.dts.wiki.service.dto.PageWatchDTO;
import com.yuzhi.dts.wiki.service.mapper.PageWatchMapper;
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
 * Integration tests for the {@link PageWatchResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class PageWatchResourceIT {

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1702275806009L);

    private static final String ENTITY_API_URL = "/api/page-watches";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PageWatchRepository pageWatchRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private PageWatchRepository pageWatchRepositoryMock;

    @Autowired
    private PageWatchMapper pageWatchMapper;

    @Mock
    private PageWatchService pageWatchServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPageWatchMockMvc;

    private PageWatch pageWatch;

    private PageWatch insertedPageWatch;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageWatch createEntity(EntityManager em) {
        PageWatch pageWatch = new PageWatch().createdAt(DEFAULT_CREATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        pageWatch.setPage(page);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        pageWatch.setUser(user);
        return pageWatch;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PageWatch createUpdatedEntity(EntityManager em) {
        PageWatch updatedPageWatch = new PageWatch().createdAt(UPDATED_CREATED_AT);
        // Add required entity
        Page page;
        if (TestUtil.findAll(em, Page.class).isEmpty()) {
            page = PageResourceIT.createUpdatedEntity(em);
            em.persist(page);
            em.flush();
        } else {
            page = TestUtil.findAll(em, Page.class).get(0);
        }
        updatedPageWatch.setPage(page);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        updatedPageWatch.setUser(user);
        return updatedPageWatch;
    }

    @BeforeEach
    void initTest() {
        pageWatch = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPageWatch != null) {
            pageWatchRepository.delete(insertedPageWatch);
            insertedPageWatch = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createPageWatch() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);
        var returnedPageWatchDTO = om.readValue(
            restPageWatchMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageWatchDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PageWatchDTO.class
        );

        // Validate the PageWatch in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPageWatch = pageWatchMapper.toEntity(returnedPageWatchDTO);
        assertPageWatchUpdatableFieldsEquals(returnedPageWatch, getPersistedPageWatch(returnedPageWatch));

        insertedPageWatch = returnedPageWatch;
    }

    @Test
    @Transactional
    void createPageWatchWithExistingId() throws Exception {
        // Create the PageWatch with an existing ID
        pageWatch.setId(1L);
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPageWatchMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageWatchDTO)))
            .andExpect(status().isBadRequest());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pageWatch.setCreatedAt(null);

        // Create the PageWatch, which fails.
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        restPageWatchMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageWatchDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPageWatches() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        // Get all the pageWatchList
        restPageWatchMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pageWatch.getId().intValue())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageWatchesWithEagerRelationshipsIsEnabled() throws Exception {
        when(pageWatchServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageWatchMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pageWatchServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPageWatchesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pageWatchServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPageWatchMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pageWatchRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPageWatch() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        // Get the pageWatch
        restPageWatchMockMvc
            .perform(get(ENTITY_API_URL_ID, pageWatch.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pageWatch.getId().intValue()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingPageWatch() throws Exception {
        // Get the pageWatch
        restPageWatchMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPageWatch() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageWatch
        PageWatch updatedPageWatch = pageWatchRepository.findById(pageWatch.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPageWatch are not directly saved in db
        em.detach(updatedPageWatch);
        updatedPageWatch.createdAt(UPDATED_CREATED_AT);
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(updatedPageWatch);

        restPageWatchMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageWatchDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isOk());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPageWatchToMatchAllProperties(updatedPageWatch);
    }

    @Test
    @Transactional
    void putNonExistingPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pageWatchDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pageWatchDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePageWatchWithPatch() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageWatch using partial update
        PageWatch partialUpdatedPageWatch = new PageWatch();
        partialUpdatedPageWatch.setId(pageWatch.getId());

        partialUpdatedPageWatch.createdAt(UPDATED_CREATED_AT);

        restPageWatchMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPageWatch.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPageWatch))
            )
            .andExpect(status().isOk());

        // Validate the PageWatch in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageWatchUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPageWatch, pageWatch),
            getPersistedPageWatch(pageWatch)
        );
    }

    @Test
    @Transactional
    void fullUpdatePageWatchWithPatch() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pageWatch using partial update
        PageWatch partialUpdatedPageWatch = new PageWatch();
        partialUpdatedPageWatch.setId(pageWatch.getId());

        partialUpdatedPageWatch.createdAt(UPDATED_CREATED_AT);

        restPageWatchMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPageWatch.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPageWatch))
            )
            .andExpect(status().isOk());

        // Validate the PageWatch in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPageWatchUpdatableFieldsEquals(partialUpdatedPageWatch, getPersistedPageWatch(partialUpdatedPageWatch));
    }

    @Test
    @Transactional
    void patchNonExistingPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, pageWatchDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPageWatch() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pageWatch.setId(longCount.incrementAndGet());

        // Create the PageWatch
        PageWatchDTO pageWatchDTO = pageWatchMapper.toDto(pageWatch);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPageWatchMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(pageWatchDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PageWatch in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePageWatch() throws Exception {
        // Initialize the database
        insertedPageWatch = pageWatchRepository.saveAndFlush(pageWatch);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the pageWatch
        restPageWatchMockMvc
            .perform(delete(ENTITY_API_URL_ID, pageWatch.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return pageWatchRepository.count();
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

    protected PageWatch getPersistedPageWatch(PageWatch pageWatch) {
        return pageWatchRepository.findById(pageWatch.getId()).orElseThrow();
    }

    protected void assertPersistedPageWatchToMatchAllProperties(PageWatch expectedPageWatch) {
        assertPageWatchAllPropertiesEquals(expectedPageWatch, getPersistedPageWatch(expectedPageWatch));
    }

    protected void assertPersistedPageWatchToMatchUpdatableProperties(PageWatch expectedPageWatch) {
        assertPageWatchAllUpdatablePropertiesEquals(expectedPageWatch, getPersistedPageWatch(expectedPageWatch));
    }
}
