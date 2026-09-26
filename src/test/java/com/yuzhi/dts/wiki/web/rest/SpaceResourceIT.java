package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.SpaceAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import com.yuzhi.dts.wiki.service.mapper.SpaceMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link SpaceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SpaceResourceIT {

    private static final String DEFAULT_SLUG = "tt9lzt9zsfr4m6e4l";
    private static final String UPDATED_SLUG = "jc61padk";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_GIT_REPO_URL = "AAAAAAAAAA";
    private static final String UPDATED_GIT_REPO_URL = "BBBBBBBBBB";

    private static final String DEFAULT_GIT_BRANCH = "AAAAAAAAAA";
    private static final String UPDATED_GIT_BRANCH = "BBBBBBBBBB";

    private static final Integer DEFAULT_POSITION = 1;
    private static final Integer UPDATED_POSITION = 2;

    private static final Boolean DEFAULT_ARCHIVED = false;
    private static final Boolean UPDATED_ARCHIVED = true;

    private static final String ENTITY_API_URL = "/api/spaces";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSpaceMockMvc;

    private Space space;

    private Space insertedSpace;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Space createEntity() {
        return new Space()
            .slug(DEFAULT_SLUG)
            .name(DEFAULT_NAME)
            .description(DEFAULT_DESCRIPTION)
            .gitRepoUrl(DEFAULT_GIT_REPO_URL)
            .gitBranch(DEFAULT_GIT_BRANCH)
            .position(DEFAULT_POSITION)
            .archived(DEFAULT_ARCHIVED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Space createUpdatedEntity() {
        return new Space()
            .slug(UPDATED_SLUG)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .gitRepoUrl(UPDATED_GIT_REPO_URL)
            .gitBranch(UPDATED_GIT_BRANCH)
            .position(UPDATED_POSITION)
            .archived(UPDATED_ARCHIVED);
    }

    @BeforeEach
    void initTest() {
        space = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedSpace != null) {
            spaceRepository.delete(insertedSpace);
            insertedSpace = null;
        }
    }

    @Test
    @Transactional
    void createSpace() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);
        var returnedSpaceDTO = om.readValue(
            restSpaceMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SpaceDTO.class
        );

        // Validate the Space in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSpace = spaceMapper.toEntity(returnedSpaceDTO);
        assertSpaceUpdatableFieldsEquals(returnedSpace, getPersistedSpace(returnedSpace));

        insertedSpace = returnedSpace;
    }

    @Test
    @Transactional
    void createSpaceWithExistingId() throws Exception {
        // Create the Space with an existing ID
        space.setId(1L);
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSpaceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        space.setSlug(null);

        // Create the Space, which fails.
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        restSpaceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        space.setName(null);

        // Create the Space, which fails.
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        restSpaceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkArchivedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        space.setArchived(null);

        // Create the Space, which fails.
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        restSpaceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSpaces() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        // Get all the spaceList
        restSpaceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(space.getId().intValue())))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].gitRepoUrl").value(hasItem(DEFAULT_GIT_REPO_URL)))
            .andExpect(jsonPath("$.[*].gitBranch").value(hasItem(DEFAULT_GIT_BRANCH)))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION)))
            .andExpect(jsonPath("$.[*].archived").value(hasItem(DEFAULT_ARCHIVED)));
    }

    @Test
    @Transactional
    void getSpace() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        // Get the space
        restSpaceMockMvc
            .perform(get(ENTITY_API_URL_ID, space.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(space.getId().intValue()))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.gitRepoUrl").value(DEFAULT_GIT_REPO_URL))
            .andExpect(jsonPath("$.gitBranch").value(DEFAULT_GIT_BRANCH))
            .andExpect(jsonPath("$.position").value(DEFAULT_POSITION))
            .andExpect(jsonPath("$.archived").value(DEFAULT_ARCHIVED));
    }

    @Test
    @Transactional
    void getNonExistingSpace() throws Exception {
        // Get the space
        restSpaceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSpace() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the space
        Space updatedSpace = spaceRepository.findById(space.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSpace are not directly saved in db
        em.detach(updatedSpace);
        updatedSpace
            .slug(UPDATED_SLUG)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .gitRepoUrl(UPDATED_GIT_REPO_URL)
            .gitBranch(UPDATED_GIT_BRANCH)
            .position(UPDATED_POSITION)
            .archived(UPDATED_ARCHIVED);
        SpaceDTO spaceDTO = spaceMapper.toDto(updatedSpace);

        restSpaceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, spaceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(spaceDTO))
            )
            .andExpect(status().isOk());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSpaceToMatchAllProperties(updatedSpace);
    }

    @Test
    @Transactional
    void putNonExistingSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, spaceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(spaceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(spaceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSpaceWithPatch() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the space using partial update
        Space partialUpdatedSpace = new Space();
        partialUpdatedSpace.setId(space.getId());

        partialUpdatedSpace.slug(UPDATED_SLUG).gitRepoUrl(UPDATED_GIT_REPO_URL).position(UPDATED_POSITION);

        restSpaceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSpace.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSpace))
            )
            .andExpect(status().isOk());

        // Validate the Space in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSpaceUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedSpace, space), getPersistedSpace(space));
    }

    @Test
    @Transactional
    void fullUpdateSpaceWithPatch() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the space using partial update
        Space partialUpdatedSpace = new Space();
        partialUpdatedSpace.setId(space.getId());

        partialUpdatedSpace
            .slug(UPDATED_SLUG)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .gitRepoUrl(UPDATED_GIT_REPO_URL)
            .gitBranch(UPDATED_GIT_BRANCH)
            .position(UPDATED_POSITION)
            .archived(UPDATED_ARCHIVED);

        restSpaceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSpace.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSpace))
            )
            .andExpect(status().isOk());

        // Validate the Space in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSpaceUpdatableFieldsEquals(partialUpdatedSpace, getPersistedSpace(partialUpdatedSpace));
    }

    @Test
    @Transactional
    void patchNonExistingSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, spaceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(spaceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(spaceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSpace() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        space.setId(longCount.incrementAndGet());

        // Create the Space
        SpaceDTO spaceDTO = spaceMapper.toDto(space);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSpaceMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(spaceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Space in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSpace() throws Exception {
        // Initialize the database
        insertedSpace = spaceRepository.saveAndFlush(space);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the space
        restSpaceMockMvc
            .perform(delete(ENTITY_API_URL_ID, space.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return spaceRepository.count();
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

    protected Space getPersistedSpace(Space space) {
        return spaceRepository.findById(space.getId()).orElseThrow();
    }

    protected void assertPersistedSpaceToMatchAllProperties(Space expectedSpace) {
        assertSpaceAllPropertiesEquals(expectedSpace, getPersistedSpace(expectedSpace));
    }

    protected void assertPersistedSpaceToMatchUpdatableProperties(Space expectedSpace) {
        assertSpaceAllUpdatablePropertiesEquals(expectedSpace, getPersistedSpace(expectedSpace));
    }
}
