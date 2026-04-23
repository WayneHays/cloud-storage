package com.waynehays.cloudfilestorage.scheduler.cleanup;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.storage.dto.UserPath;
import com.waynehays.cloudfilestorage.resource.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.quota.service.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.service.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
            log.error("Cleanup: failed to release quotas", e);
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
        List<UserPath> userPaths = files.stream()
                .map(f -> new UserPath(f.userId(), f.path()))
                .toList();
        storageService.deleteObjects(userPaths);
    }

    private void releaseQuotas(List<ResourceMetadataDto> files) {
        List<SpaceReleaseDto> spaceToRelease = files.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                ResourceMetadataDto::userId,
                                Collectors.summingLong(ResourceMetadataDto::size)
                        ),
                        map -> map.entrySet().stream()
                                .map(entry -> new SpaceReleaseDto(
                                        entry.getKey(), entry.getValue()))
                                .toList()
                ));
        quotaService.batchReleaseUsedSpace(spaceToRelease);
    }

    private void deleteMetadata(List<ResourceMetadataDto> files) {
        List<Long> ids = files.stream()
                .map(ResourceMetadataDto::id)
                .toList();
        metadataService.deleteByIds(ids);
    }
}
