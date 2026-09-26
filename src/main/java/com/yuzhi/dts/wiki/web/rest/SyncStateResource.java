package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import com.yuzhi.dts.wiki.service.SyncStateService;
import com.yuzhi.dts.wiki.service.dto.SyncStateDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.SyncState}.
 */
@RestController
@RequestMapping("/api/sync-states")
public class SyncStateResource {

    private static final Logger LOG = LoggerFactory.getLogger(SyncStateResource.class);

    private static final String ENTITY_NAME = "syncState";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final SyncStateService syncStateService;

    private final SyncStateRepository syncStateRepository;

    public SyncStateResource(SyncStateService syncStateService, SyncStateRepository syncStateRepository) {
        this.syncStateService = syncStateService;
        this.syncStateRepository = syncStateRepository;
    }

    /**
     * {@code POST  /sync-states} : Create a new syncState.
     *
     * @param syncStateDTO the syncStateDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new syncStateDTO, or with status {@code 400 (Bad Request)} if the syncState has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SyncStateDTO> createSyncState(@Valid @RequestBody SyncStateDTO syncStateDTO) throws URISyntaxException {
        LOG.debug("REST request to save SyncState : {}", syncStateDTO);
        if (syncStateDTO.getId() != null) {
            throw new BadRequestAlertException("A new syncState cannot already have an ID", ENTITY_NAME, "idexists");
        }
        syncStateDTO = syncStateService.save(syncStateDTO);
        return ResponseEntity.created(new URI("/api/sync-states/" + syncStateDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, syncStateDTO.getId().toString()))
            .body(syncStateDTO);
    }

    /**
     * {@code PUT  /sync-states/:id} : Updates an existing syncState.
     *
     * @param id the id of the syncStateDTO to save.
     * @param syncStateDTO the syncStateDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncStateDTO,
     * or with status {@code 400 (Bad Request)} if the syncStateDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the syncStateDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SyncStateDTO> updateSyncState(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SyncStateDTO syncStateDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SyncState : {}, {}", id, syncStateDTO);
        if (syncStateDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncStateDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncStateRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        syncStateDTO = syncStateService.update(syncStateDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncStateDTO.getId().toString()))
            .body(syncStateDTO);
    }

    /**
     * {@code PATCH  /sync-states/:id} : Partial updates given fields of an existing syncState, field will ignore if it is null
     *
     * @param id the id of the syncStateDTO to save.
     * @param syncStateDTO the syncStateDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncStateDTO,
     * or with status {@code 400 (Bad Request)} if the syncStateDTO is not valid,
     * or with status {@code 404 (Not Found)} if the syncStateDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the syncStateDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SyncStateDTO> partialUpdateSyncState(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SyncStateDTO syncStateDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SyncState partially : {}, {}", id, syncStateDTO);
        if (syncStateDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncStateDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncStateRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SyncStateDTO> result = syncStateService.partialUpdate(syncStateDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncStateDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /sync-states} : get all the Sync States.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Sync States in body.
     */
    @GetMapping("")
    public List<SyncStateDTO> getAllSyncStates() {
        LOG.debug("REST request to get all SyncStates");
        return syncStateService.findAll();
    }

    /**
     * {@code GET  /sync-states/:id} : get the "id" syncState.
     *
     * @param id the id of the syncStateDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the syncStateDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncStateDTO> getSyncState(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SyncState : {}", id);
        Optional<SyncStateDTO> syncStateDTO = syncStateService.findOne(id);
        return ResponseUtil.wrapOrNotFound(syncStateDTO);
    }

    /**
     * {@code DELETE  /sync-states/:id} : delete the "id" syncState.
     *
     * @param id the id of the syncStateDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSyncState(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SyncState : {}", id);
        syncStateService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
