package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.PageWatchDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.PageWatch}.
 */
public interface PageWatchService {
    /**
     * Save a pageWatch.
     *
     * @param pageWatchDTO the entity to save.
     * @return the persisted entity.
     */
    PageWatchDTO save(PageWatchDTO pageWatchDTO);

    /**
     * Updates a pageWatch.
     *
     * @param pageWatchDTO the entity to update.
     * @return the persisted entity.
     */
    PageWatchDTO update(PageWatchDTO pageWatchDTO);

    /**
     * Partially updates a pageWatch.
     *
     * @param pageWatchDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<PageWatchDTO> partialUpdate(PageWatchDTO pageWatchDTO);

    /**
     * Get all the pageWatches.
     *
     * @return the list of entities.
     */
    List<PageWatchDTO> findAll();

    /**
     * Get all the pageWatches with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<PageWatchDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" pageWatch.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<PageWatchDTO> findOne(Long id);

    /**
     * Delete the "id" pageWatch.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
