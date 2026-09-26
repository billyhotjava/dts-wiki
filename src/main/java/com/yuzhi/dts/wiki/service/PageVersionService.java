package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.PageVersion}.
 */
public interface PageVersionService {
    /**
     * Save a pageVersion.
     *
     * @param pageVersionDTO the entity to save.
     * @return the persisted entity.
     */
    PageVersionDTO save(PageVersionDTO pageVersionDTO);

    /**
     * Updates a pageVersion.
     *
     * @param pageVersionDTO the entity to update.
     * @return the persisted entity.
     */
    PageVersionDTO update(PageVersionDTO pageVersionDTO);

    /**
     * Partially updates a pageVersion.
     *
     * @param pageVersionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<PageVersionDTO> partialUpdate(PageVersionDTO pageVersionDTO);

    /**
     * Get all the pageVersions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<PageVersionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" pageVersion.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<PageVersionDTO> findOne(Long id);

    /**
     * Delete the "id" pageVersion.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
