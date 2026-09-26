package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.PageDraftDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.PageDraft}.
 */
public interface PageDraftService {
    /**
     * Save a pageDraft.
     *
     * @param pageDraftDTO the entity to save.
     * @return the persisted entity.
     */
    PageDraftDTO save(PageDraftDTO pageDraftDTO);

    /**
     * Updates a pageDraft.
     *
     * @param pageDraftDTO the entity to update.
     * @return the persisted entity.
     */
    PageDraftDTO update(PageDraftDTO pageDraftDTO);

    /**
     * Partially updates a pageDraft.
     *
     * @param pageDraftDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<PageDraftDTO> partialUpdate(PageDraftDTO pageDraftDTO);

    /**
     * Get all the pageDrafts.
     *
     * @return the list of entities.
     */
    List<PageDraftDTO> findAll();

    /**
     * Get all the pageDrafts with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<PageDraftDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" pageDraft.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<PageDraftDTO> findOne(Long id);

    /**
     * Delete the "id" pageDraft.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
