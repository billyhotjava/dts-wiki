package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.PageDraftRepository;
import com.yuzhi.dts.wiki.service.PageDraftService;
import com.yuzhi.dts.wiki.service.dto.PageDraftDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.PageDraft}.
 */
@RestController
@RequestMapping("/api/page-drafts")
public class PageDraftResource {

    private static final Logger LOG = LoggerFactory.getLogger(PageDraftResource.class);

    private static final String ENTITY_NAME = "pageDraft";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final PageDraftService pageDraftService;

    private final PageDraftRepository pageDraftRepository;

    public PageDraftResource(PageDraftService pageDraftService, PageDraftRepository pageDraftRepository) {
        this.pageDraftService = pageDraftService;
        this.pageDraftRepository = pageDraftRepository;
    }

    /**
     * {@code POST  /page-drafts} : Create a new pageDraft.
     *
     * @param pageDraftDTO the pageDraftDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new pageDraftDTO, or with status {@code 400 (Bad Request)} if the pageDraft has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PageDraftDTO> createPageDraft(@Valid @RequestBody PageDraftDTO pageDraftDTO) throws URISyntaxException {
        LOG.debug("REST request to save PageDraft : {}", pageDraftDTO);
        if (pageDraftDTO.getId() != null) {
            throw new BadRequestAlertException("A new pageDraft cannot already have an ID", ENTITY_NAME, "idexists");
        }
        pageDraftDTO = pageDraftService.save(pageDraftDTO);
        return ResponseEntity.created(new URI("/api/page-drafts/" + pageDraftDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, pageDraftDTO.getId().toString()))
            .body(pageDraftDTO);
    }

    /**
     * {@code PUT  /page-drafts/:id} : Updates an existing pageDraft.
     *
     * @param id the id of the pageDraftDTO to save.
     * @param pageDraftDTO the pageDraftDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageDraftDTO,
     * or with status {@code 400 (Bad Request)} if the pageDraftDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the pageDraftDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PageDraftDTO> updatePageDraft(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PageDraftDTO pageDraftDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PageDraft : {}, {}", id, pageDraftDTO);
        if (pageDraftDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageDraftDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageDraftRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        pageDraftDTO = pageDraftService.update(pageDraftDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageDraftDTO.getId().toString()))
            .body(pageDraftDTO);
    }

    /**
     * {@code PATCH  /page-drafts/:id} : Partial updates given fields of an existing pageDraft, field will ignore if it is null
     *
     * @param id the id of the pageDraftDTO to save.
     * @param pageDraftDTO the pageDraftDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageDraftDTO,
     * or with status {@code 400 (Bad Request)} if the pageDraftDTO is not valid,
     * or with status {@code 404 (Not Found)} if the pageDraftDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the pageDraftDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PageDraftDTO> partialUpdatePageDraft(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PageDraftDTO pageDraftDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PageDraft partially : {}, {}", id, pageDraftDTO);
        if (pageDraftDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageDraftDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageDraftRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PageDraftDTO> result = pageDraftService.partialUpdate(pageDraftDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageDraftDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /page-drafts} : get all the Page Drafts.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Page Drafts in body.
     */
    @GetMapping("")
    public List<PageDraftDTO> getAllPageDrafts(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all PageDrafts");
        return pageDraftService.findAll();
    }

    /**
     * {@code GET  /page-drafts/:id} : get the "id" pageDraft.
     *
     * @param id the id of the pageDraftDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pageDraftDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageDraftDTO> getPageDraft(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PageDraft : {}", id);
        Optional<PageDraftDTO> pageDraftDTO = pageDraftService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pageDraftDTO);
    }

    /**
     * {@code DELETE  /page-drafts/:id} : delete the "id" pageDraft.
     *
     * @param id the id of the pageDraftDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePageDraft(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PageDraft : {}", id);
        pageDraftService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
