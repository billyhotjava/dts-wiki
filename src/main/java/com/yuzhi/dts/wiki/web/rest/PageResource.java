package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.PageQueryService;
import com.yuzhi.dts.wiki.service.PageService;
import com.yuzhi.dts.wiki.service.criteria.PageCriteria;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.Page}.
 */
@RestController
@RequestMapping("/api/pages")
public class PageResource {

    private static final Logger LOG = LoggerFactory.getLogger(PageResource.class);

    private static final String ENTITY_NAME = "page";

    @Value("${jhipster.clientApp.name:dtsWiki}")
    private String applicationName;

    private final PageService pageService;

    private final PageRepository pageRepository;

    private final PageQueryService pageQueryService;

    public PageResource(PageService pageService, PageRepository pageRepository, PageQueryService pageQueryService) {
        this.pageService = pageService;
        this.pageRepository = pageRepository;
        this.pageQueryService = pageQueryService;
    }

    /**
     * {@code POST  /pages} : Create a new page.
     *
     * @param pageDTO the pageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new pageDTO, or with status {@code 400 (Bad Request)} if the page has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PageDTO> createPage(@Valid @RequestBody PageDTO pageDTO) throws URISyntaxException {
        LOG.debug("REST request to save Page : {}", pageDTO);
        if (pageDTO.getId() != null) {
            throw new BadRequestAlertException("A new page cannot already have an ID", ENTITY_NAME, "idexists");
        }
        pageDTO = pageService.save(pageDTO);
        return ResponseEntity.created(new URI("/api/pages/" + pageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, pageDTO.getId().toString()))
            .body(pageDTO);
    }

    /**
     * {@code PUT  /pages/:id} : Updates an existing page.
     *
     * @param id the id of the pageDTO to save.
     * @param pageDTO the pageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageDTO,
     * or with status {@code 400 (Bad Request)} if the pageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the pageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PageDTO> updatePage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PageDTO pageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Page : {}, {}", id, pageDTO);
        if (pageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        pageDTO = pageService.update(pageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageDTO.getId().toString()))
            .body(pageDTO);
    }

    /**
     * {@code PATCH  /pages/:id} : Partial updates given fields of an existing page, field will ignore if it is null
     *
     * @param id the id of the pageDTO to save.
     * @param pageDTO the pageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated pageDTO,
     * or with status {@code 400 (Bad Request)} if the pageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the pageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the pageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PageDTO> partialUpdatePage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PageDTO pageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Page partially : {}, {}", id, pageDTO);
        if (pageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PageDTO> result = pageService.partialUpdate(pageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /pages} : get all the Pages.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Pages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PageDTO>> getAllPages(
        PageCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Pages by criteria: {}", criteria);

        Page<PageDTO> page = pageQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /pages/count} : count all the pages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPages(PageCriteria criteria) {
        LOG.debug("REST request to count Pages by criteria: {}", criteria);
        return ResponseEntity.ok().body(pageQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /pages/:id} : get the "id" page.
     *
     * @param id the id of the pageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageDTO> getPage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Page : {}", id);
        Optional<PageDTO> pageDTO = pageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pageDTO);
    }

    /**
     * {@code DELETE  /pages/:id} : delete the "id" page.
     *
     * @param id the id of the pageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Page : {}", id);
        pageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
