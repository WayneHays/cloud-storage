package com.waynehays.cloudfilestorage.service.cleanup;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
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
            log.error("Orphans cleanup job failed", e);
        }
    }

    private void processOrphans() {
        List<ResourceMetadataDto> orphans = metadataService.findMarkedForDeletion();

        if (orphans.isEmpty()) {
            return;
        }

        log.info("Cleanup started: {} orphans found", orphans.size());
        int cleaned = 0;

        for (ResourceMetadataDto orphan : orphans) {
            try {
                cleanOrphan(orphan);
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to cleanup orphan: {}", orphan.path(), e);
            }
        }

        log.info("Orphan cleanup completed: {}/{} orphans removed", cleaned, orphans.size());
    }

    private void cleanOrphan(ResourceMetadataDto orphan) {
        if (orphan.isFile()) {
            deleteFromStorage(orphan);
            quotaService.releaseSpace(orphan.userId(), orphan.size());
        }
        metadataService.deleteById(orphan.id());
    }

    private void deleteFromStorage(ResourceMetadataDto orphan) {
        String storageKey = keyResolver.resolveKey(orphan.userId(), orphan.path());
        resourceStorage.deleteObject(storageKey);
    }
}
