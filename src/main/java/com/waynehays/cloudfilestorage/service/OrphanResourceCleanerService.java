package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanResourceCleanerService {
    private final ResourceStorageApi storage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    public void clean() {
        try {
            processOrphans();
        } catch (Exception e) {
            log.error("Cleanup job failed", e);
        }
    }

    private void processOrphans() {
        List<ResourceMetadata> orphans = metadataService.findMarkedForDeletion();

        if (orphans.isEmpty()) {
            return;
        }

        log.info("Cleanup started: {} orphans found", orphans.size());
        int cleaned = 0;

        for (ResourceMetadata orphan : orphans) {
            try {
                String storageKey = keyResolver.resolveKey(orphan.getUserId(), orphan.getPath());
                storage.deleteObject(storageKey);
                metadataService.deleteById(orphan.getId());
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to clean orphan: {}", orphan.getPath(), e);
            }
        }

        log.info("Cleanup completed: {}/{} orphans processed", cleaned, orphans.size());
    }
}
