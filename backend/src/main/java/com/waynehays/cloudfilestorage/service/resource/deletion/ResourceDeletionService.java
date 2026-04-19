package com.waynehays.cloudfilestorage.service.resource.deletion;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeletionService implements ResourceDeletionServiceApi {
    private final ResourceStorageServiceApi storageService;
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
        log.info("Start delete file: userId={}, path={}", userId, path);

        metadataService.markForDeletion(userId, path);
        storageService.deleteObject(userId, path);
        metadataService.deleteFileByPath(userId, path);
        quotaService.releaseSpace(userId, size);

        log.info("Finished delete file: userId={}, path={}", userId, path);
    }

    private void deleteDirectory(Long userId, String path) {
        log.info("Start delete directory: userId={}, path={}", userId, path);

        long totalSize = metadataService.markDirectoryForDeletionAndSumSize(userId, path);
        storageService.deleteDirectory(userId, path);
        metadataService.deleteDirectoryMetadata(userId, path);

        if (totalSize > 0) {
            quotaService.releaseSpace(userId, totalSize);
        }

        log.info("Finished delete directory: userId={}, path={}", userId, path);
    }
}
