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
    private final ContentManifestService manifest;
    private final ReentrantLock inventoryLock = new ReentrantLock();
    private final ConcurrentHashMap<String, ReentrantLock> spaceLocks = new ConcurrentHashMap<>();

    public GitSyncScheduler(SpaceRepository spaceRepository, SyncCycleService cycleService, ContentManifestService manifest) {
        this.spaceRepository = spaceRepository;
        this.cycleService = cycleService;
        this.manifest = manifest;
    }

    @Scheduled(fixedDelayString = "#{${application.wiki.sync-interval-seconds:30} * 1000}")
    @SchedulerLock(name = "git-sync-cycle", lockAtLeastFor = "10s")
    public void run() {
        inventoryLock.lock();
        try {
            manifest.refresh();
            for (Space space : spaceRepository.findAll()) {
                if (space.getGitRepoUrl() != null && !space.getGitRepoUrl().isBlank()) { syncValidatedSpace(space.getSlug()); }
            }
        } catch (RuntimeException e) {
            LOG.warn("Content inventory rejected; no spaces were synchronized: {}", e.getClass().getSimpleName());
        } finally { inventoryLock.unlock(); }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onReady(org.springframework.boot.context.event.ApplicationReadyEvent event) {
        if (event.getApplicationContext().getEnvironment().getProperty("wiki.scheduling.enabled", Boolean.class, true)) { run(); }
    }

    /**
     * Single-space cycle (admin trigger or scheduler loop). Guarded by a JVM-local
     * per-space lock (v1 is single-instance; the distributed ShedLock guards the loop).
     */
    public void syncSpace(String slug) {
        inventoryLock.lock();
        try { manifest.refresh(); syncValidatedSpace(slug); }
        finally { inventoryLock.unlock(); }
    }

    private void syncValidatedSpace(String slug) {
        var lock = spaceLocks.computeIfAbsent(slug, k -> new ReentrantLock());
        lock.lock();
        try {
            cycleService.cycleSpace(slug);
        } finally {
            lock.unlock();
        }
    }
}
