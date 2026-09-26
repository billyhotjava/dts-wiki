package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.SyncRoot}.
 */
public interface SyncRootService {
    /**
     * Save a syncRoot.
     *
     * @param syncRootDTO the entity to save.
     * @return the persisted entity.
     */
    SyncRootDTO save(SyncRootDTO syncRootDTO);

    /**
     * Updates a syncRoot.
     *
     * @param syncRootDTO the entity to update.
     * @return the persisted entity.
     */
    SyncRootDTO update(SyncRootDTO syncRootDTO);

    /**
     * Partially updates a syncRoot.
     *
     * @param syncRootDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SyncRootDTO> partialUpdate(SyncRootDTO syncRootDTO);

    /**
     * Get all the syncRoots.
     *
     * @return the list of entities.
     */
    List<SyncRootDTO> findAll();

    /**
     * Get all the SyncRootDTO where SyncState is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<SyncRootDTO> findAllWhereSyncStateIsNull();

    /**
     * Get all the syncRoots with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SyncRootDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" syncRoot.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SyncRootDTO> findOne(Long id);

    /**
     * Delete the "id" syncRoot.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
