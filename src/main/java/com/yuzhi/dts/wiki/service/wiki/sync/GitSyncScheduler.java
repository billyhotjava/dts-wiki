package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.yuzhi.dts.wiki.repository.SpaceRepository;

/**
 * Scheduler loop + per-space guards. The cycle itself lives in
 * {@link SyncCycleService} so the atomic confirm step runs in a real transaction
 * (self-invocation would bypass the proxy).
 */
@Service
public class GitSyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(GitSyncScheduler.class);

    private final SpaceRepository spaceRepository;
    private final SyncCycleService cycleService;
    private final ConcurrentHashMap<String, ReentrantLock> spaceLocks = new ConcurrentHashMap<>();

    public GitSyncScheduler(SpaceRepository spaceRepository, SyncCycleService cycleService) {
        this.spaceRepository = spaceRepository;
        this.cycleService = cycleService;
    }

    @Scheduled(fixedDelayString = "#{${application.wiki.sync-interval-seconds:30} * 1000}")
    @SchedulerLock(name = "git-sync-cycle", lockAtLeastFor = "10s")
    public void run() {
        for (Space space : spaceRepository.findAll()) {
            if (space.getGitRepoUrl() == null || space.getGitRepoUrl().isBlank()) {
                continue;
            }
            syncSpace(space.getSlug());
        }
    }

    /**
     * Single-space cycle (admin trigger or scheduler loop). Guarded by a JVM-local
     * per-space lock (v1 is single-instance; the distributed ShedLock guards the loop).
     */
    public void syncSpace(String slug) {
        var lock = spaceLocks.computeIfAbsent(slug, k -> new ReentrantLock());
        lock.lock();
        try {
            cycleService.cycleSpace(slug);
        } finally {
            lock.unlock();
        }
    }
}
