package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Space;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Space entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {
    // DTS-WIKI: customized (Sprint-6 W4).
    Optional<Space> findOneBySlug(String slug);
}
