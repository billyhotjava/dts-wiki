package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.service.SyncConflictService;
import com.yuzhi.dts.wiki.service.dto.SyncConflictDTO;
import com.yuzhi.dts.wiki.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.SyncConflict}.
 */
@RestController
@RequestMapping("/api/sync-conflicts")
public class SyncConflictResource {

    private static final Logger LOG = LoggerFactory.getLogger(SyncConflictResource.class);

    private static final String ENTITY_NAME = "syncConflict";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final SyncConflictService syncConflictService;

    private final SyncConflictRepository syncConflictRepository;

    public SyncConflictResource(SyncConflictService syncConflictService, SyncConflictRepository syncConflictRepository) {
        this.syncConflictService = syncConflictService;
        this.syncConflictRepository = syncConflictRepository;
    }

    /**
     * {@code POST  /sync-conflicts} : Create a new syncConflict.
     *
     * @param syncConflictDTO the syncConflictDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new syncConflictDTO, or with status {@code 400 (Bad Request)} if the syncConflict has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SyncConflictDTO> createSyncConflict(@Valid @RequestBody SyncConflictDTO syncConflictDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SyncConflict : {}", syncConflictDTO);
        if (syncConflictDTO.getId() != null) {
            throw new BadRequestAlertException("A new syncConflict cannot already have an ID", ENTITY_NAME, "idexists");
        }
        syncConflictDTO = syncConflictService.save(syncConflictDTO);
        return ResponseEntity.created(new URI("/api/sync-conflicts/" + syncConflictDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, syncConflictDTO.getId().toString()))
            .body(syncConflictDTO);
    }

    /**
     * {@code PUT  /sync-conflicts/:id} : Updates an existing syncConflict.
     *
     * @param id the id of the syncConflictDTO to save.
     * @param syncConflictDTO the syncConflictDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncConflictDTO,
     * or with status {@code 400 (Bad Request)} if the syncConflictDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the syncConflictDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SyncConflictDTO> updateSyncConflict(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SyncConflictDTO syncConflictDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SyncConflict : {}, {}", id, syncConflictDTO);
        if (syncConflictDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncConflictDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncConflictRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        syncConflictDTO = syncConflictService.update(syncConflictDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncConflictDTO.getId().toString()))
            .body(syncConflictDTO);
    }

    /**
     * {@code PATCH  /sync-conflicts/:id} : Partial updates given fields of an existing syncConflict, field will ignore if it is null
     *
     * @param id the id of the syncConflictDTO to save.
     * @param syncConflictDTO the syncConflictDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncConflictDTO,
     * or with status {@code 400 (Bad Request)} if the syncConflictDTO is not valid,
     * or with status {@code 404 (Not Found)} if the syncConflictDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the syncConflictDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SyncConflictDTO> partialUpdateSyncConflict(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SyncConflictDTO syncConflictDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SyncConflict partially : {}, {}", id, syncConflictDTO);
        if (syncConflictDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncConflictDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncConflictRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SyncConflictDTO> result = syncConflictService.partialUpdate(syncConflictDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncConflictDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /sync-conflicts} : get all the Sync Conflicts.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Sync Conflicts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SyncConflictDTO>> getAllSyncConflicts(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of SyncConflicts");
        Page<SyncConflictDTO> page;
        if (eagerload) {
            page = syncConflictService.findAllWithEagerRelationships(pageable);
        } else {
            page = syncConflictService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /sync-conflicts/:id} : get the "id" syncConflict.
     *
     * @param id the id of the syncConflictDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the syncConflictDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncConflictDTO> getSyncConflict(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SyncConflict : {}", id);
        Optional<SyncConflictDTO> syncConflictDTO = syncConflictService.findOne(id);
        return ResponseUtil.wrapOrNotFound(syncConflictDTO);
    }

    /**
     * {@code DELETE  /sync-conflicts/:id} : delete the "id" syncConflict.
     *
     * @param id the id of the syncConflictDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSyncConflict(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SyncConflict : {}", id);
        syncConflictService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
