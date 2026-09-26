package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.service.SyncRootService;
import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.SyncRoot}.
 */
@RestController
@RequestMapping("/api/sync-roots")
public class SyncRootResource {

    private static final Logger LOG = LoggerFactory.getLogger(SyncRootResource.class);

    private static final String ENTITY_NAME = "syncRoot";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final SyncRootService syncRootService;

    private final SyncRootRepository syncRootRepository;

    public SyncRootResource(SyncRootService syncRootService, SyncRootRepository syncRootRepository) {
        this.syncRootService = syncRootService;
        this.syncRootRepository = syncRootRepository;
    }

    /**
     * {@code POST  /sync-roots} : Create a new syncRoot.
     *
     * @param syncRootDTO the syncRootDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new syncRootDTO, or with status {@code 400 (Bad Request)} if the syncRoot has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SyncRootDTO> createSyncRoot(@Valid @RequestBody SyncRootDTO syncRootDTO) throws URISyntaxException {
        LOG.debug("REST request to save SyncRoot : {}", syncRootDTO);
        if (syncRootDTO.getId() != null) {
            throw new BadRequestAlertException("A new syncRoot cannot already have an ID", ENTITY_NAME, "idexists");
        }
        syncRootDTO = syncRootService.save(syncRootDTO);
        return ResponseEntity.created(new URI("/api/sync-roots/" + syncRootDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, syncRootDTO.getId().toString()))
            .body(syncRootDTO);
    }

    /**
     * {@code PUT  /sync-roots/:id} : Updates an existing syncRoot.
     *
     * @param id the id of the syncRootDTO to save.
     * @param syncRootDTO the syncRootDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncRootDTO,
     * or with status {@code 400 (Bad Request)} if the syncRootDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the syncRootDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SyncRootDTO> updateSyncRoot(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SyncRootDTO syncRootDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SyncRoot : {}, {}", id, syncRootDTO);
        if (syncRootDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncRootDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncRootRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        syncRootDTO = syncRootService.update(syncRootDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncRootDTO.getId().toString()))
            .body(syncRootDTO);
    }

    /**
     * {@code PATCH  /sync-roots/:id} : Partial updates given fields of an existing syncRoot, field will ignore if it is null
     *
     * @param id the id of the syncRootDTO to save.
     * @param syncRootDTO the syncRootDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated syncRootDTO,
     * or with status {@code 400 (Bad Request)} if the syncRootDTO is not valid,
     * or with status {@code 404 (Not Found)} if the syncRootDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the syncRootDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SyncRootDTO> partialUpdateSyncRoot(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SyncRootDTO syncRootDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SyncRoot partially : {}, {}", id, syncRootDTO);
        if (syncRootDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, syncRootDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!syncRootRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SyncRootDTO> result = syncRootService.partialUpdate(syncRootDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, syncRootDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /sync-roots} : get all the Sync Roots.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Sync Roots in body.
     */
    @GetMapping("")
    public List<SyncRootDTO> getAllSyncRoots(
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        if ("syncstate-is-null".equals(filter)) {
            LOG.debug("REST request to get all SyncRoots where syncState is null");
            return syncRootService.findAllWhereSyncStateIsNull();
        }
        LOG.debug("REST request to get all SyncRoots");
        return syncRootService.findAll();
    }

    /**
     * {@code GET  /sync-roots/:id} : get the "id" syncRoot.
     *
     * @param id the id of the syncRootDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the syncRootDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncRootDTO> getSyncRoot(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SyncRoot : {}", id);
        Optional<SyncRootDTO> syncRootDTO = syncRootService.findOne(id);
        return ResponseUtil.wrapOrNotFound(syncRootDTO);
    }

    /**
     * {@code DELETE  /sync-roots/:id} : delete the "id" syncRoot.
     *
     * @param id the id of the syncRootDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSyncRoot(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SyncRoot : {}", id);
        syncRootService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
