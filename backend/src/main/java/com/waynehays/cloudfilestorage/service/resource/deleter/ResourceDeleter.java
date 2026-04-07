package com.waynehays.cloudfilestorage.service.resource.deleter;

import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeleter implements ResourceDeleterApi {
    private final ResourceStorageService storageService;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void delete(Long userId, String path) {
        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, path);

        if (metadata.isFile()) {
            deleteFile(userId, path, metadata.size());
        } else {
            deleteDirectory(userId, path);
        }
    }

    private void deleteFile(Long userId, String path, Long size) {
        log.info("Start deleting file: userId={}, path={}", userId, path);

        storageService.deleteObject(userId, path);
        metadataService.deleteByPath(userId, path);
        quotaService.releaseSpace(userId, size);

        log.info("Successfully deleted file: userId={}, path={}", userId, path);
    }

    private void deleteDirectory(Long userId, String path) {
        log.info("Start delete directory: userId={}, path={}", userId, path);

        long totalSize = metadataService.sumFileSizesByPathPrefix(userId, path);
        metadataService.markForDeletionByPathPrefix(userId, path);
        storageService.deleteDirectory(userId, path);
        metadataService.deleteByPathPrefix(userId, path);

        if (totalSize > 0) {
            quotaService.releaseSpace(userId, totalSize);
        }

        log.info("Successfully deleted directory: userId={}, path={}", userId, path);
    }
}
