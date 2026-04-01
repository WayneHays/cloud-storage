package com.waynehays.cloudfilestorage.service.resource.deleter;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeleter implements ResourceDeleterApi {
    private final ResourceStorageApi resourceStorage;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void delete(Long userId, String path) {
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, path);
        String objectKey = keyResolver.resolveKey(userId, path);

        if (PathUtils.isFile(path)) {
            deleteFile(userId, path, objectKey, dto.size());
        } else {
            deleteDirectory(userId, path, objectKey);
        }
    }

    private void deleteFile(Long userId, String path, String objectKey, long size) {
        log.info("Start delete file: userId={}, path={}", userId, path);

        metadataService.markForDeletion(userId, path);
        resourceStorage.deleteObject(objectKey);
        metadataService.delete(userId, path);
        quotaService.releaseSpace(userId, size);

        log.info("Successfully deleted file: userId={}, path={}", userId, path);
    }

    private void deleteDirectory(Long userId, String path, String objectKey) {
        log.info("Start delete directory: userId={}, path={}", userId, path);

        long totalSize = metadataService.sumResourceSizesByPrefix(userId, path);
        metadataService.markForDeletionByPrefix(userId, path);
        resourceStorage.deleteByPrefix(objectKey);
        metadataService.deleteByPrefix(userId, path);

        if (totalSize > 0) {
            quotaService.releaseSpace(userId, totalSize);
        }

        log.info("Successfully deleted directory: userId={}, path={}", userId, path);
    }
}
