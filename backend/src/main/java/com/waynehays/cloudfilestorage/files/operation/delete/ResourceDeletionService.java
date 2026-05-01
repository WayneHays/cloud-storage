package com.waynehays.cloudfilestorage.files.operation.delete;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

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
            deleteFile(userId, metadata);
        } else {
            deleteDirectory(userId, path);
        }
    }

    private void deleteFile(Long userId, ResourceMetadataDto metadata) {
        log.info("Start delete file: {}", metadata.path());

        metadataService.markForDeletion(userId, metadata.path());
        storageService.deleteObject(userId, metadata.storageKey());
        metadataService.deleteFileByPath(userId, metadata.path());
        quotaService.releaseSpace(userId, metadata.size());

        log.info("Finished delete file: {}", metadata.path());
    }

    private void deleteDirectory(Long userId, String path) {
        log.info("Start delete directory with content: {}", path);

        DeleteDirectoryResult result = metadataService.markDirectoryForDeletionAndCollectKeys(userId, path);

        if (result.hasKeys()) {
            storageService.deleteObjects(Map.of(userId, result.storageKeys()));
        }

        metadataService.deleteDirectoryMetadata(userId, path);

        if (result.totalSize() > 0) {
            quotaService.releaseSpace(userId, result.totalSize());
        }

        log.info("Finished delete directory with content: {}", path);
    }
}
