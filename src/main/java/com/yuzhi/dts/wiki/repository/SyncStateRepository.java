package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.SyncState;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SyncState entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SyncStateRepository extends JpaRepository<SyncState, Long> {}
