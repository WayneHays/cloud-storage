package com.waynehays.cloudfilestorage.service.cleanup;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanResourceCleanerService {
    private final ResourceStorageApi resourceStorage;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageKeyResolverApi keyResolver;
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
                cleanOrphan(orphan);
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to clean orphan: {}", orphan.getPath(), e);
            }
        }

        log.info("Cleanup completed: {}/{} orphans processed", cleaned, orphans.size());
    }

    private void cleanOrphan(ResourceMetadata orphan) {
        deleteFromStorage(orphan);
        metadataService.deleteById(orphan.getId());
        releaseSpaceIfFile(orphan);
    }

    private void deleteFromStorage(ResourceMetadata orphan) {
        String storageKey = keyResolver.resolveKey(orphan.getUserId(), orphan.getPath());
        resourceStorage.deleteObject(storageKey);
    }

    private void releaseSpaceIfFile(ResourceMetadata orphan) {
        if (orphan.isFile()) {
            quotaService.releaseSpace(orphan.getUserId(), orphan.getSize());
        }
    }
}
