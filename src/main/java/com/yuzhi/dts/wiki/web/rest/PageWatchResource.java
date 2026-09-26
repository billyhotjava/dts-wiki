package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.PageWatchRepository;
import com.yuzhi.dts.wiki.service.PageWatchService;
import com.yuzhi.dts.wiki.service.dto.PageWatchDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.PageWatch}.
 */
@RestController
@RequestMapping("/api/page-watches")
public class PageWatchResource {

    private static final Logger LOG = LoggerFactory.getLogger(PageWatchResource.class);

    private static final String ENTITY_NAME = "pageWatch";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final PageWatchService pageWatchService;

    private final PageWatchRepository pageWatchRepository;

    public PageWatchResource(PageWatchService pageWatchService, PageWatchRepository pageWatchRepository) {
        this.pageWatchService = pageWatchService;
        this.pageWatchRepository = pageWatchRepository;
    }

    /**
     * {@code POST  /page-watches} : Create a new pageWatch.
     *
     * @param pageWatchDTO the pageWatchDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new pageWatchDTO, or with status {@code 400 (Bad Request)} if the pageWatch has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PageWatchDTO> createPageWatch(@Valid @RequestBody PageWatchDTO pageWatchDTO) throws URISyntaxException {
        LOG.debug("REST request to save PageWatch : {}", pageWatchDTO);
        if (pageWatchDTO.getId() != null) {
            throw new BadRequestAlertException("A new pageWatch cannot already have an ID", ENTITY_NAME, "idexists");
        }
        pageWatchDTO = pageWatchService.save(pageWatchDTO);
        return ResponseEntity.created(new URI("/api/page-watches/" + pageWatchDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, pageWatchDTO.getId().toString()))
            .body(pageWatchDTO);
    }

    /**
     * {@code PUT  /page-watches/:id} : Updates an existing pageWatch.
     *
     * @param id the id of the pageWatchDTO to save.
     * @param pageWatchDTO the pageWatchDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageWatchDTO,
     * or with status {@code 400 (Bad Request)} if the pageWatchDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the pageWatchDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PageWatchDTO> updatePageWatch(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PageWatchDTO pageWatchDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PageWatch : {}, {}", id, pageWatchDTO);
        if (pageWatchDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageWatchDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageWatchRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        pageWatchDTO = pageWatchService.update(pageWatchDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageWatchDTO.getId().toString()))
            .body(pageWatchDTO);
    }

    /**
     * {@code PATCH  /page-watches/:id} : Partial updates given fields of an existing pageWatch, field will ignore if it is null
     *
     * @param id the id of the pageWatchDTO to save.
     * @param pageWatchDTO the pageWatchDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageWatchDTO,
     * or with status {@code 400 (Bad Request)} if the pageWatchDTO is not valid,
     * or with status {@code 404 (Not Found)} if the pageWatchDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the pageWatchDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PageWatchDTO> partialUpdatePageWatch(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PageWatchDTO pageWatchDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PageWatch partially : {}, {}", id, pageWatchDTO);
        if (pageWatchDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageWatchDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageWatchRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PageWatchDTO> result = pageWatchService.partialUpdate(pageWatchDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageWatchDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /page-watches} : get all the Page Watches.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Page Watches in body.
     */
    @GetMapping("")
    public List<PageWatchDTO> getAllPageWatches(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all PageWatches");
        return pageWatchService.findAll();
    }

    /**
     * {@code GET  /page-watches/:id} : get the "id" pageWatch.
     *
     * @param id the id of the pageWatchDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pageWatchDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageWatchDTO> getPageWatch(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PageWatch : {}", id);
        Optional<PageWatchDTO> pageWatchDTO = pageWatchService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pageWatchDTO);
    }

    /**
     * {@code DELETE  /page-watches/:id} : delete the "id" pageWatch.
     *
     * @param id the id of the pageWatchDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePageWatch(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PageWatch : {}", id);
        pageWatchService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
