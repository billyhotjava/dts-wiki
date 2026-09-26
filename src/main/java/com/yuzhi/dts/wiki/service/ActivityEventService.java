package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.service.dto.ActivityEventDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.yuzhi.dts.wiki.domain.ActivityEvent}.
 */
public interface ActivityEventService {
    /**
     * Save a activityEvent.
     *
     * @param activityEventDTO the entity to save.
     * @return the persisted entity.
     */
    ActivityEventDTO save(ActivityEventDTO activityEventDTO);

    /**
     * Updates a activityEvent.
     *
     * @param activityEventDTO the entity to update.
     * @return the persisted entity.
     */
    ActivityEventDTO update(ActivityEventDTO activityEventDTO);

    /**
     * Partially updates a activityEvent.
     *
     * @param activityEventDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ActivityEventDTO> partialUpdate(ActivityEventDTO activityEventDTO);

    /**
     * Get all the activityEvents with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ActivityEventDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" activityEvent.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ActivityEventDTO> findOne(Long id);

    /**
     * Delete the "id" activityEvent.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
