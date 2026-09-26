package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.LabelDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.Label}.
 */
public interface LabelService {
    /**
     * Save a label.
     *
     * @param labelDTO the entity to save.
     * @return the persisted entity.
     */
    LabelDTO save(LabelDTO labelDTO);

    /**
     * Updates a label.
     *
     * @param labelDTO the entity to update.
     * @return the persisted entity.
     */
    LabelDTO update(LabelDTO labelDTO);

    /**
     * Partially updates a label.
     *
     * @param labelDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<LabelDTO> partialUpdate(LabelDTO labelDTO);

    /**
     * Get all the labels.
     *
     * @return the list of entities.
     */
    List<LabelDTO> findAll();

    /**
     * Get the "id" label.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<LabelDTO> findOne(Long id);

    /**
     * Delete the "id" label.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
