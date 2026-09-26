package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.service.ActivityEventQueryService;
import com.yuzhi.dts.wiki.service.ActivityEventService;
import com.yuzhi.dts.wiki.service.criteria.ActivityEventCriteria;
import com.yuzhi.dts.wiki.service.dto.ActivityEventDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.ActivityEvent}.
 */
@RestController
@RequestMapping("/api/activity-events")
public class ActivityEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityEventResource.class);

    private final ActivityEventService activityEventService;

    private final ActivityEventQueryService activityEventQueryService;

    public ActivityEventResource(ActivityEventService activityEventService, ActivityEventQueryService activityEventQueryService) {
        this.activityEventService = activityEventService;
        this.activityEventQueryService = activityEventQueryService;
    }

    /**
     * {@code GET  /activity-events} : get all the Activity Events.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Activity Events in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ActivityEventDTO>> getAllActivityEvents(
        ActivityEventCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ActivityEvents by criteria: {}", criteria);

        Page<ActivityEventDTO> page = activityEventQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /activity-events/count} : count all the activityEvents.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countActivityEvents(ActivityEventCriteria criteria) {
        LOG.debug("REST request to count ActivityEvents by criteria: {}", criteria);
        return ResponseEntity.ok().body(activityEventQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /activity-events/:id} : get the "id" activityEvent.
     *
     * @param id the id of the activityEventDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the activityEventDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityEventDTO> getActivityEvent(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ActivityEvent : {}", id);
        Optional<ActivityEventDTO> activityEventDTO = activityEventService.findOne(id);
        return ResponseUtil.wrapOrNotFound(activityEventDTO);
    }
}
