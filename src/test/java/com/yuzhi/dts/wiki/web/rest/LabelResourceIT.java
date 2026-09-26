package com.yuzhi.dts.wiki.web.rest;

import static com.yuzhi.dts.wiki.domain.LabelAsserts.*;
import static com.yuzhi.dts.wiki.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Label;
import com.yuzhi.dts.wiki.repository.LabelRepository;
import com.yuzhi.dts.wiki.service.dto.LabelDTO;
import com.yuzhi.dts.wiki.service.mapper.LabelMapper;
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
 * Integration tests for the {@link LabelResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
// DTS-WIKI: customized (Sprint-6 design 03 S3): generated entity endpoints are ROLE_ADMIN only.
@WithMockUser(authorities = { "ROLE_ADMIN" })
class LabelResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/labels";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private LabelMapper labelMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLabelMockMvc;

    private Label label;

    private Label insertedLabel;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Label createEntity() {
        return new Label().name(DEFAULT_NAME);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Label createUpdatedEntity() {
        return new Label().name(UPDATED_NAME);
    }

    @BeforeEach
    void initTest() {
        label = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLabel != null) {
            labelRepository.delete(insertedLabel);
            insertedLabel = null;
        }
    }

    @Test
    @Transactional
    void createLabel() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);
        var returnedLabelDTO = om.readValue(
            restLabelMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(labelDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LabelDTO.class
        );

        // Validate the Label in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLabel = labelMapper.toEntity(returnedLabelDTO);
        assertLabelUpdatableFieldsEquals(returnedLabel, getPersistedLabel(returnedLabel));

        insertedLabel = returnedLabel;
    }

    @Test
    @Transactional
    void createLabelWithExistingId() throws Exception {
        // Create the Label with an existing ID
        label.setId(1L);
        LabelDTO labelDTO = labelMapper.toDto(label);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLabelMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(labelDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        label.setName(null);

        // Create the Label, which fails.
        LabelDTO labelDTO = labelMapper.toDto(label);

        restLabelMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(labelDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLabels() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        // Get all the labelList
        restLabelMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(label.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    @Test
    @Transactional
    void getLabel() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        // Get the label
        restLabelMockMvc
            .perform(get(ENTITY_API_URL_ID, label.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(label.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME));
    }

    @Test
    @Transactional
    void getNonExistingLabel() throws Exception {
        // Get the label
        restLabelMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLabel() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the label
        Label updatedLabel = labelRepository.findById(label.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLabel are not directly saved in db
        em.detach(updatedLabel);
        updatedLabel.name(UPDATED_NAME);
        LabelDTO labelDTO = labelMapper.toDto(updatedLabel);

        restLabelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, labelDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(labelDTO))
            )
            .andExpect(status().isOk());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLabelToMatchAllProperties(updatedLabel);
    }

    @Test
    @Transactional
    void putNonExistingLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, labelDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(labelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(labelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(labelDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLabelWithPatch() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the label using partial update
        Label partialUpdatedLabel = new Label();
        partialUpdatedLabel.setId(label.getId());

        partialUpdatedLabel.name(UPDATED_NAME);

        restLabelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLabel.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLabel))
            )
            .andExpect(status().isOk());

        // Validate the Label in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLabelUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLabel, label), getPersistedLabel(label));
    }

    @Test
    @Transactional
    void fullUpdateLabelWithPatch() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the label using partial update
        Label partialUpdatedLabel = new Label();
        partialUpdatedLabel.setId(label.getId());

        partialUpdatedLabel.name(UPDATED_NAME);

        restLabelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLabel.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLabel))
            )
            .andExpect(status().isOk());

        // Validate the Label in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLabelUpdatableFieldsEquals(partialUpdatedLabel, getPersistedLabel(partialUpdatedLabel));
    }

    @Test
    @Transactional
    void patchNonExistingLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, labelDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(labelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(labelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLabel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        label.setId(longCount.incrementAndGet());

        // Create the Label
        LabelDTO labelDTO = labelMapper.toDto(label);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLabelMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(labelDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Label in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLabel() throws Exception {
        // Initialize the database
        insertedLabel = labelRepository.saveAndFlush(label);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the label
        restLabelMockMvc
            .perform(delete(ENTITY_API_URL_ID, label.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return labelRepository.count();
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

    protected Label getPersistedLabel(Label label) {
        return labelRepository.findById(label.getId()).orElseThrow();
    }

    protected void assertPersistedLabelToMatchAllProperties(Label expectedLabel) {
        assertLabelAllPropertiesEquals(expectedLabel, getPersistedLabel(expectedLabel));
    }

    protected void assertPersistedLabelToMatchUpdatableProperties(Label expectedLabel) {
        assertLabelAllUpdatablePropertiesEquals(expectedLabel, getPersistedLabel(expectedLabel));
    }
}
