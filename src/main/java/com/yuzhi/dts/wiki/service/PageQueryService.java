package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.domain.*; // for static metamodels
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.criteria.PageCriteria;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.mapper.PageMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// DTS-WIKI: customized (entity named Page clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Page} entities in the database.
 * The main input is a {@link PageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PageQueryService extends QueryService<Page> {

    private static final Logger LOG = LoggerFactory.getLogger(PageQueryService.class);

    private final PageRepository pageRepository;

    private final PageMapper pageMapper;

    public PageQueryService(PageRepository pageRepository, PageMapper pageMapper) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
    }

    /**
     * Return a {@link Page} of {@link PageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PageDTO> findByCriteria(PageCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Page> specification = createSpecification(criteria);
        return pageRepository.fetchBagRelationships(pageRepository.findAll(specification, page)).map(pageMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Page> specification = createSpecification(criteria);
        return pageRepository.count(specification);
    }

    /**
     * Function to convert {@link PageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Page> createSpecification(PageCriteria criteria) {
        Specification<Page> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Page_.currentVersion, JoinType.LEFT);
                root.fetch(Page_.space, JoinType.LEFT);
                root.fetch(Page_.parent, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Page_.id),
                    buildStringSpecification(criteria.getTitle(), Page_.title),
                    buildSpecification(criteria.getKind(), Page_.kind),
                    buildStringSpecification(criteria.getGitPath(), Page_.gitPath),
                    buildRangeSpecification(criteria.getPosition(), Page_.position),
                    buildSpecification(criteria.getSyncStatus(), Page_.syncStatus),
                    buildRangeSpecification(criteria.getCreatedAt(), Page_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Page_.updatedAt),
                    buildRangeSpecification(criteria.getDeletedAt(), Page_.deletedAt),
                    buildSpecification(criteria.getChildrenId(), root -> root.join(Page_.childrens, JoinType.LEFT).get(Page_.id)),
                    buildSpecification(criteria.getVersionsId(), root -> root.join(Page_.versionses, JoinType.LEFT).get(PageVersion_.id)),
                    buildSpecification(criteria.getAttachmentsId(), root ->
                        root.join(Page_.attachmentses, JoinType.LEFT).get(Attachment_.id)
                    ),
                    buildSpecification(criteria.getCommentsId(), root -> root.join(Page_.commentses, JoinType.LEFT).get(Comment_.id)),
                    buildSpecification(criteria.getCurrentVersionId(), root ->
                        root.join(Page_.currentVersion, JoinType.LEFT).get(PageVersion_.id)
                    ),
                    buildSpecification(criteria.getLabelsId(), root -> root.join(Page_.labelses, JoinType.LEFT).get(Label_.id)),
                    buildSpecification(criteria.getSpaceId(), root -> root.join(Page_.space, JoinType.LEFT).get(Space_.id)),
                    buildSpecification(criteria.getParentId(), root -> root.join(Page_.parent, JoinType.LEFT).get(Page_.id))
                )
            );
        }
        return specification;
    }
}
