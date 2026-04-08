package com.waynehays.cloudfilestorage.jobs.deletion;

import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDeletionService implements ScheduledDeletionServiceApi {
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageService storageService;
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

        deleteFromStorage(files);
        releaseQuotas(files);
        deleteMetadata(files);

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
