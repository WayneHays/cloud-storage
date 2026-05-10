package com.waynehays.cloudfilestorage.files.operation.cleanup;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaBatchApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class CleanupService implements CleanupServiceApi {
    private final StorageQuotaBatchApi quotaBatchService;
    private final ResourceStorageServiceApi storageService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void processDeletedFiles(int limit) {
        log.info("Cleanup started");
        int totalCleaned = 0;
        int cleaned;

        do {
            cleaned = executeCleanup(limit);
            totalCleaned += cleaned;
        } while (cleaned == limit);

        log.info("Cleanup completed: {} resources removed", totalCleaned);
    }

    private int executeCleanup(int limit) {
        List<ResourceMetadataDto> files = metadataService.findFilesMarkedForDeletion(limit);

        if (files.isEmpty()) {
            return 0;
        }

        try {
            deleteFromStorage(files);
        } catch (Exception e) {
            log.error("Cleanup: failed to delete files from storage", e);
            return 0;
        }

        try {
            releaseQuotas(files);
        } catch (Exception e) {
            log.error("Cleanup: failed to release quotas — will retry", e);
            return 0;
        }

        try {
            deleteMetadata(files);
        } catch (Exception e) {
            log.error("Cleanup: failed to delete metadata", e);
            return 0;
        }

        return files.size();
    }

    private void deleteFromStorage(List<ResourceMetadataDto> files) {
        Map<Long, List<String>> byUser = files.stream()
                .collect(Collectors.groupingBy(
                        ResourceMetadataDto::userId,
                        Collectors.mapping(ResourceMetadataDto::storageKey, Collectors.toList())
                ));
        storageService.deleteObjects(byUser);
    }
    
    private void releaseQuotas(List<ResourceMetadataDto> files) {
        List<SpaceReleaseDto> spaceToRelease = aggregateByUser(files);
        quotaBatchService.batchReleaseUsedSpace(spaceToRelease);
    }

    private List<SpaceReleaseDto> aggregateByUser(List<ResourceMetadataDto> files) {
        return files.stream()
                .collect(Collectors.groupingBy(
                        ResourceMetadataDto::userId,
                        Collectors.summingLong(ResourceMetadataDto::size)
                ))
                .entrySet().stream()
                .map(e -> new SpaceReleaseDto(e.getKey(), e.getValue()))
                .toList();
    }

    private void deleteMetadata(List<ResourceMetadataDto> files) {
        List<Long> ids = files.stream()
                .map(ResourceMetadataDto::id)
                .toList();
        metadataService.deleteByIds(ids);
    }
}
