package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.PageDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.Page}.
 */
public interface PageService {
    /**
     * Save a page.
     *
     * @param pageDTO the entity to save.
     * @return the persisted entity.
     */
    PageDTO save(PageDTO pageDTO);

    /**
     * Updates a page.
     *
     * @param pageDTO the entity to update.
     * @return the persisted entity.
     */
    PageDTO update(PageDTO pageDTO);

    /**
     * Partially updates a page.
     *
     * @param pageDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<PageDTO> partialUpdate(PageDTO pageDTO);

    /**
     * Get all the pages with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<PageDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" page.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<PageDTO> findOne(Long id);

    /**
     * Delete the "id" page.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
