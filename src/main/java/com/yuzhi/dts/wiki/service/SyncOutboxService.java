package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.SyncOutboxDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.SyncOutbox}.
 */
public interface SyncOutboxService {
    /**
     * Save a syncOutbox.
     *
     * @param syncOutboxDTO the entity to save.
     * @return the persisted entity.
     */
    SyncOutboxDTO save(SyncOutboxDTO syncOutboxDTO);

    /**
     * Updates a syncOutbox.
     *
     * @param syncOutboxDTO the entity to update.
     * @return the persisted entity.
     */
    SyncOutboxDTO update(SyncOutboxDTO syncOutboxDTO);

    /**
     * Partially updates a syncOutbox.
     *
     * @param syncOutboxDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SyncOutboxDTO> partialUpdate(SyncOutboxDTO syncOutboxDTO);

    /**
     * Get all the syncOutboxes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SyncOutboxDTO> findAll(Pageable pageable);

    /**
     * Get all the syncOutboxes with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SyncOutboxDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" syncOutbox.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SyncOutboxDTO> findOne(Long id);

    /**
     * Delete the "id" syncOutbox.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
