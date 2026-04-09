package com.waynehays.cloudfilestorage.service.scheduler.cleanup;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService implements CleanupServiceApi {
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageServiceApi storageService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void clean(int limit) {
        log.info("Deletion cleanup started");
        int totalCleaned = 0;

        try {
            int cleaned;
            do {
                cleaned = executeCleanup(limit);
                totalCleaned += cleaned;
            } while (cleaned == limit);
        } catch (Exception e) {
            log.error("Deletion cleanup failed", e);
        }

        log.info("Deletion cleanup completed: {} resources removed", totalCleaned);
    }

    private int executeCleanup(int batchSize) {
        List<ResourceMetadataDto> files = metadataService.findFilesMarkedForDeletion(batchSize);

        if (files.isEmpty()) {
            return 0;
        }

        try {
            deleteFromStorage(files);
        } catch (Exception e) {
            log.error("Failed to delete files from storage", e);
            return 0;
        }

        try {
            releaseQuotas(files);
        } catch (Exception e) {
            log.error("Failed to release quotas", e);
        }

        try {
            deleteMetadata(files);
        } catch (Exception e) {
            log.error("Failed to delete metadata", e);
            return 0;
        }

        return files.size();
    }

    private void deleteFromStorage(List<ResourceMetadataDto> files) {
        Map<Long, List<String>> pathsByUserId = files.stream()
                .collect(Collectors.groupingBy(
                        ResourceMetadataDto::userId,
                        Collectors.mapping(ResourceMetadataDto::path, Collectors.toList())
                ));
        pathsByUserId.forEach(storageService::deleteObjects);
    }

    private void releaseQuotas(List<ResourceMetadataDto> files) {
        List<SpaceReleaseDto> releases = files.stream()
                .collect(Collectors.groupingBy(
                        ResourceMetadataDto::userId,
                        Collectors.summingLong(ResourceMetadataDto::size)
                ))
                .entrySet().stream()
                .map(e -> new SpaceReleaseDto(e.getKey(), e.getValue()))
                .toList();
        quotaService.batchDecreaseUsedSpace(releases);
    }

    private void deleteMetadata(List<ResourceMetadataDto> files) {
        List<Long> ids = files.stream()
                .map(ResourceMetadataDto::id)
                .toList();
        metadataService.deleteAllByIds(ids);
    }
}
