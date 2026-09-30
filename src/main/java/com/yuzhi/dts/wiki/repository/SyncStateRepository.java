package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.SyncState;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SyncState entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
    // DTS-WIKI: customized (Sprint-6 W6).
    Optional<SyncState> findOneBySyncRootId(Long syncRootId);
}
