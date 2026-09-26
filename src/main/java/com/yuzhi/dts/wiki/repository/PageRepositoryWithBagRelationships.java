package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Page;
// DTS-WIKI: customized (entity named Page clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import java.util.List;
import java.util.Optional;

public interface PageRepositoryWithBagRelationships {
    Optional<Page> fetchBagRelationships(Optional<Page> page);

    List<Page> fetchBagRelationships(List<Page> pages);

    org.springframework.data.domain.Page<Page> fetchBagRelationships(org.springframework.data.domain.Page<Page> pages);
}
