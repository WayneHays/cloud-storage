package com.waynehays.cloudfilestorage.service.resource.deleter;

import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolver;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeleter implements ResourceDeleterApi {
    private final ResourceStorageApi resourceStorage;
    private final ResourceStorageKeyResolver keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void delete(Long userId, String path) {
        metadataService.findOrThrow(userId, path);
        String objectKey = keyResolver.resolveKey(userId, path);

        if (PathUtils.isFile(path)) {
            deleteFile(userId, path, objectKey);
        } else {
            deleteDirectory(userId, path, objectKey);
        }
    }

    private void deleteFile(Long userId, String path, String objectKey) {
        log.info("Start delete file: userId={}, path={}", userId, path);

        metadataService.markForDeletion(userId, path);
        resourceStorage.deleteObject(objectKey);
        metadataService.delete(userId, path);

        log.info("Successfully deleted file: userId={}, path={}", userId, path);
    }

    private void deleteDirectory(Long userId, String path, String objectKey) {
        log.info("Start delete directory: userId={}, path={}", userId, path);

        metadataService.markForDeletionByPrefix(userId, path);
        resourceStorage.deleteByPrefix(objectKey);
        metadataService.deleteByPrefix(userId, path);

        log.info("Successfully deleted directory: userId={}, path={}", userId, path);
    }
}
