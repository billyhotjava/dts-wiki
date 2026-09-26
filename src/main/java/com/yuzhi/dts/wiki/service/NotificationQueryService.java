package com.yuzhi.dts.wiki.service;

import com.yuzhi.dts.wiki.domain.*; // for static metamodels
import com.yuzhi.dts.wiki.domain.Notification;
import com.yuzhi.dts.wiki.repository.NotificationRepository;
import com.yuzhi.dts.wiki.service.criteria.NotificationCriteria;
import com.yuzhi.dts.wiki.service.dto.NotificationDTO;
import com.yuzhi.dts.wiki.service.mapper.NotificationMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// DTS-WIKI: customized (domain.* pulls in entity Page which clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Notification} entities in the database.
 * The main input is a {@link NotificationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link NotificationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class NotificationQueryService extends QueryService<Notification> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationQueryService.class);

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    public NotificationQueryService(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    /**
     * Return a {@link Page} of {@link NotificationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NotificationDTO> findByCriteria(NotificationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Notification> specification = createSpecification(criteria);
        return notificationRepository.findAll(specification, page).map(notificationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(NotificationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Notification> specification = createSpecification(criteria);
        return notificationRepository.count(specification);
    }

    /**
     * Function to convert {@link NotificationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Notification> createSpecification(NotificationCriteria criteria) {
        Specification<Notification> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Notification_.recipient, JoinType.LEFT);
                root.fetch(Notification_.page, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Notification_.id),
                    buildSpecification(criteria.getType(), Notification_.type),
                    buildStringSpecification(criteria.getTitle(), Notification_.title),
                    buildStringSpecification(criteria.getLink(), Notification_.link),
                    buildRangeSpecification(criteria.getCreatedAt(), Notification_.createdAt),
                    buildRangeSpecification(criteria.getReadAt(), Notification_.readAt),
                    buildSpecification(criteria.getRecipientId(), root -> root.join(Notification_.recipient, JoinType.LEFT).get(User_.id)),
                    buildSpecification(criteria.getPageId(), root -> root.join(Notification_.page, JoinType.LEFT).get(Page_.id))
                )
            );
        }
        return specification;
    }
}
