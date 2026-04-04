package com.waynehays.cloudfilestorage.service.maintenance;

import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.provider.ResourceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanStorageCleanerService {
    private final ResourceStorageService storageService;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    public void clean(int limit) {
        try {
            processOrphans(limit);
        } catch (Exception e) {
            log.error("Storage orphans cleanup job failed", e);
        }
    }

    private void processOrphans(int limit) {
        List<ResourceMetadataDto> orphans = metadataService.findMarkedForDeletion(limit);

        if (orphans.isEmpty()) {
            return;
        }

        log.info("Storage orphans cleanup started: {} orphans found", orphans.size());
        int cleaned = 0;

        for (ResourceMetadataDto orphan : orphans) {
            try {
                cleanOrphan(orphan);
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to cleanup storage orphan: {}", orphan.path(), e);
            }
        }

        log.info("Storage orphan cleanup completed: {}/{} orphans removed", cleaned, orphans.size());
    }

    private void cleanOrphan(ResourceMetadataDto orphan) {
        if (orphan.isFile()) {
            storageService.deleteObject(orphan.userId(), orphan.path());
            quotaService.releaseSpace(orphan.userId(), orphan.size());
        }
        metadataService.deleteById(orphan.id());
    }
}
