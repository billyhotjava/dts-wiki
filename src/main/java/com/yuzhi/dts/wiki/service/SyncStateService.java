package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.SyncStateDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.SyncState}.
 */
public interface SyncStateService {
    /**
     * Save a syncState.
     *
     * @param syncStateDTO the entity to save.
     * @return the persisted entity.
     */
    SyncStateDTO save(SyncStateDTO syncStateDTO);

    /**
     * Updates a syncState.
     *
     * @param syncStateDTO the entity to update.
     * @return the persisted entity.
     */
    SyncStateDTO update(SyncStateDTO syncStateDTO);

    /**
     * Partially updates a syncState.
     *
     * @param syncStateDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SyncStateDTO> partialUpdate(SyncStateDTO syncStateDTO);

    /**
     * Get all the syncStates.
     *
     * @return the list of entities.
     */
    List<SyncStateDTO> findAll();

    /**
     * Get the "id" syncState.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SyncStateDTO> findOne(Long id);

    /**
     * Delete the "id" syncState.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
