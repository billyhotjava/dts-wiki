package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.domain.*; // for static metamodels
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.service.criteria.PageVersionCriteria;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import com.yuzhi.dts.wiki.service.mapper.PageVersionMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link PageVersion} entities in the database.
 * The main input is a {@link PageVersionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PageVersionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PageVersionQueryService extends QueryService<PageVersion> {

    private static final Logger LOG = LoggerFactory.getLogger(PageVersionQueryService.class);

    private final PageVersionRepository pageVersionRepository;

    private final PageVersionMapper pageVersionMapper;

    public PageVersionQueryService(PageVersionRepository pageVersionRepository, PageVersionMapper pageVersionMapper) {
        this.pageVersionRepository = pageVersionRepository;
        this.pageVersionMapper = pageVersionMapper;
    }

    /**
     * Return a {@link Page} of {@link PageVersionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PageVersionDTO> findByCriteria(PageVersionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PageVersion> specification = createSpecification(criteria);
        return pageVersionRepository.findAll(specification, page).map(pageVersionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PageVersionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PageVersion> specification = createSpecification(criteria);
        return pageVersionRepository.count(specification);
    }

    /**
     * Function to convert {@link PageVersionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PageVersion> createSpecification(PageVersionCriteria criteria) {
        Specification<PageVersion> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(PageVersion_.author, JoinType.LEFT);
                root.fetch(PageVersion_.page, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), PageVersion_.id),
                    buildRangeSpecification(criteria.getVersionNo(), PageVersion_.versionNo),
                    buildStringSpecification(criteria.getContentSha256(), PageVersion_.contentSha256),
                    buildStringSpecification(criteria.getAuthorName(), PageVersion_.authorName),
                    buildStringSpecification(criteria.getAuthorEmail(), PageVersion_.authorEmail),
                    buildSpecification(criteria.getSource(), PageVersion_.source),
                    buildStringSpecification(criteria.getGitCommit(), PageVersion_.gitCommit),
                    buildStringSpecification(criteria.getMessage(), PageVersion_.message),
                    buildRangeSpecification(criteria.getCreatedAt(), PageVersion_.createdAt),
                    buildSpecification(criteria.getAuthorId(), root -> root.join(PageVersion_.author, JoinType.LEFT).get(User_.id)),
                    buildSpecification(criteria.getPageId(), root -> root.join(PageVersion_.page, JoinType.LEFT).get(Page_.id))
                )
            );
        }
        return specification;
    }
}
