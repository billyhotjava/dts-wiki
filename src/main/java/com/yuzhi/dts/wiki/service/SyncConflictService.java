package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.SyncConflictDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.SyncConflict}.
 */
public interface SyncConflictService {
    /**
     * Save a syncConflict.
     *
     * @param syncConflictDTO the entity to save.
     * @return the persisted entity.
     */
    SyncConflictDTO save(SyncConflictDTO syncConflictDTO);

    /**
     * Updates a syncConflict.
     *
     * @param syncConflictDTO the entity to update.
     * @return the persisted entity.
     */
    SyncConflictDTO update(SyncConflictDTO syncConflictDTO);

    /**
     * Partially updates a syncConflict.
     *
     * @param syncConflictDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SyncConflictDTO> partialUpdate(SyncConflictDTO syncConflictDTO);

    /**
     * Get all the syncConflicts.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SyncConflictDTO> findAll(Pageable pageable);

    /**
     * Get all the syncConflicts with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SyncConflictDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" syncConflict.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SyncConflictDTO> findOne(Long id);

    /**
     * Delete the "id" syncConflict.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
