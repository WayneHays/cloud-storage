package com.waynehays.cloudfilestorage.files.operation.delete;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceDeletionService implements ResourceDeletionServiceApi {
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
        log.info("Start delete file: {}", path);

        metadataService.markForDeletion(userId, path);
        storageService.deleteObject(userId, path);
        metadataService.deleteFileByPath(userId, path);
        quotaService.releaseSpace(userId, size);

        log.info("Finished delete file: {}", path);
    }

    private void deleteDirectory(Long userId, String path) {
        log.info("Start delete directory with content: {}", path);

        long totalSize = metadataService.markDirectoryForDeletionAndSumSize(userId, path);
        storageService.deleteByPrefix(userId, path);
        metadataService.deleteDirectoryMetadata(userId, path);

        if (totalSize > 0) {
            quotaService.releaseSpace(userId, totalSize);
        }

        log.info("Finished delete directory with content: {}", path);
    }
}
