package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Page;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
// DTS-WIKI: customized (entity named Page clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class PageRepositoryWithBagRelationshipsImpl implements PageRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String PAGES_PARAMETER = "pages";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Page> fetchBagRelationships(Optional<Page> page) {
        return page.map(this::fetchLabelses);
    }

    @Override
    public org.springframework.data.domain.Page<Page> fetchBagRelationships(org.springframework.data.domain.Page<Page> pages) {
        return new PageImpl<>(fetchBagRelationships(pages.getContent()), pages.getPageable(), pages.getTotalElements());
    }

    @Override
    public List<Page> fetchBagRelationships(List<Page> pages) {
        return Optional.of(pages).map(this::fetchLabelses).orElse(List.of());
    }

    Page fetchLabelses(Page result) {
        return entityManager
            .createQuery("select page from Page page left join fetch page.labelses where page.id = :id", Page.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Page> fetchLabelses(List<Page> pages) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, pages.size()).forEach(index -> order.put(pages.get(index).getId(), index));
        List<Page> result = entityManager
            .createQuery("select page from Page page left join fetch page.labelses where page in :pages", Page.class)
            .setParameter(PAGES_PARAMETER, pages)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
