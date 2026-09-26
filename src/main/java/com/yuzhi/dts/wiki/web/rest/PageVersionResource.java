package com.yuzhi.dts.wiki.web.rest;

import com.yuzhi.dts.wiki.service.PageVersionQueryService;
import com.yuzhi.dts.wiki.service.PageVersionService;
import com.yuzhi.dts.wiki.service.criteria.PageVersionCriteria;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
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
 * REST controller for managing {@link com.yuzhi.dts.wiki.domain.PageVersion}.
 */
@RestController
@RequestMapping("/api/page-versions")
public class PageVersionResource {

    private static final Logger LOG = LoggerFactory.getLogger(PageVersionResource.class);

    private final PageVersionService pageVersionService;

    private final PageVersionQueryService pageVersionQueryService;

    public PageVersionResource(PageVersionService pageVersionService, PageVersionQueryService pageVersionQueryService) {
        this.pageVersionService = pageVersionService;
        this.pageVersionQueryService = pageVersionQueryService;
    }

    /**
     * {@code GET  /page-versions} : get all the Page Versions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Page Versions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PageVersionDTO>> getAllPageVersions(
        PageVersionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PageVersions by criteria: {}", criteria);

        Page<PageVersionDTO> page = pageVersionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /page-versions/count} : count all the pageVersions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPageVersions(PageVersionCriteria criteria) {
        LOG.debug("REST request to count PageVersions by criteria: {}", criteria);
        return ResponseEntity.ok().body(pageVersionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /page-versions/:id} : get the "id" pageVersion.
     *
     * @param id the id of the pageVersionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the pageVersionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageVersionDTO> getPageVersion(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PageVersion : {}", id);
        Optional<PageVersionDTO> pageVersionDTO = pageVersionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(pageVersionDTO);
    }
}
