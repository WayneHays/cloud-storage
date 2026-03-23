package com.waynehays.cloudfilestorage.sheduler;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanResourceCleaner {
    private static final String LOG_CLEANUP_FAILED = "Cleanup job failed";
    private static final String LOG_FAILED_CLEAN_ORPHAN = "Failed to clean orphan: {}";
    private static final String LOG_SUCCESSFUL_CLEANUP = "Cleanup completed: {}/{} orphans processed";

    private final ResourceStorageApi storage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Scheduled(fixedRateString = "${cleanup.interval}")
    public void clean() {
        try {
            processOrphans();
        } catch (Exception e) {
            log.error(LOG_CLEANUP_FAILED, e);
        }
    }

    private void processOrphans() {
        List<ResourceMetadata> orphans = metadataService.findMarkedForDeletion();

        if (orphans.isEmpty()) {
            return;
        }

        int cleaned = 0;

        for (ResourceMetadata orphan : orphans) {
            try {
                String storageKey = keyResolver.resolveKey(orphan.getUserId(), orphan.getPath());
                storage.delete(storageKey);
                metadataService.deleteById(orphan.getId());
                cleaned++;
            } catch (Exception e) {
                log.warn(LOG_FAILED_CLEAN_ORPHAN, orphan.getPath(), e);
            }
        }

        log.info(LOG_SUCCESSFUL_CLEANUP, cleaned, orphans.size());
    }
}
