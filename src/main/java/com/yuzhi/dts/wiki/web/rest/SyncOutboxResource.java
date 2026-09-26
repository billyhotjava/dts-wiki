package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.SyncOutboxService;
import com.yuzhi.dts.wiki.service.dto.SyncOutboxDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.SyncOutbox}.
 */
@RestController
@RequestMapping("/api/sync-outboxes")
public class SyncOutboxResource {

    private static final Logger LOG = LoggerFactory.getLogger(SyncOutboxResource.class);

    private static final String ENTITY_NAME = "syncOutbox";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final SyncOutboxService syncOutboxService;

    private final SyncOutboxRepository syncOutboxRepository;

    public SyncOutboxResource(SyncOutboxService syncOutboxService, SyncOutboxRepository syncOutboxRepository) {
        this.syncOutboxService = syncOutboxService;
        this.syncOutboxRepository = syncOutboxRepository;
    }

    /**
     * {@code POST  /sync-outboxes} : Create a new syncOutbox.
     *
     * @param syncOutboxDTO the syncOutboxDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new syncOutboxDTO, or with status {@code 400 (Bad Request)} if the syncOutbox has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SyncOutboxDTO> createSyncOutbox(@Valid @RequestBody SyncOutboxDTO syncOutboxDTO) throws URISyntaxException {
        LOG.debug("REST request to save SyncOutbox : {}", syncOutboxDTO);
        if (syncOutboxDTO.getId() != null) {
            throw new BadRequestAlertException("A new syncOutbox cannot already have an ID", ENTITY_NAME, "idexists");
        }
        syncOutboxDTO = syncOutboxService.save(syncOutboxDTO);
        return ResponseEntity.created(new URI("/api/sync-outboxes/" + syncOutboxDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, syncOutboxDTO.getId().toString()))
            .body(syncOutboxDTO);
    }

    /**
     * {@code PUT  /sync-outboxes/:id} : Updates an existing syncOutbox.
     *
     * @param id the id of the syncOutboxDTO to save.
     * @param syncOutboxDTO the syncOutboxDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncOutboxDTO,
     * or with status {@code 400 (Bad Request)} if the syncOutboxDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the syncOutboxDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SyncOutboxDTO> updateSyncOutbox(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SyncOutboxDTO syncOutboxDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SyncOutbox : {}, {}", id, syncOutboxDTO);
        if (syncOutboxDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncOutboxDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncOutboxRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        syncOutboxDTO = syncOutboxService.update(syncOutboxDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncOutboxDTO.getId().toString()))
            .body(syncOutboxDTO);
    }

    /**
     * {@code PATCH  /sync-outboxes/:id} : Partial updates given fields of an existing syncOutbox, field will ignore if it is null
     *
     * @param id the id of the syncOutboxDTO to save.
     * @param syncOutboxDTO the syncOutboxDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncOutboxDTO,
     * or with status {@code 400 (Bad Request)} if the syncOutboxDTO is not valid,
     * or with status {@code 404 (Not Found)} if the syncOutboxDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the syncOutboxDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SyncOutboxDTO> partialUpdateSyncOutbox(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SyncOutboxDTO syncOutboxDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SyncOutbox partially : {}, {}", id, syncOutboxDTO);
        if (syncOutboxDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncOutboxDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncOutboxRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SyncOutboxDTO> result = syncOutboxService.partialUpdate(syncOutboxDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncOutboxDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /sync-outboxes} : get all the Sync Outboxes.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Sync Outboxes in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SyncOutboxDTO>> getAllSyncOutboxes(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of SyncOutboxes");
        Page<SyncOutboxDTO> page;
        if (eagerload) {
            page = syncOutboxService.findAllWithEagerRelationships(pageable);
        } else {
            page = syncOutboxService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /sync-outboxes/:id} : get the "id" syncOutbox.
     *
     * @param id the id of the syncOutboxDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the syncOutboxDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncOutboxDTO> getSyncOutbox(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SyncOutbox : {}", id);
        Optional<SyncOutboxDTO> syncOutboxDTO = syncOutboxService.findOne(id);
        return ResponseUtil.wrapOrNotFound(syncOutboxDTO);
    }

    /**
     * {@code DELETE  /sync-outboxes/:id} : delete the "id" syncOutbox.
     *
     * @param id the id of the syncOutboxDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSyncOutbox(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SyncOutbox : {}", id);
        syncOutboxService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
