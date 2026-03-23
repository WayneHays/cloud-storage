package com.waynehays.cloudfilestorage.service.resource.deleter;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceDeleter implements ResourceDeleterApi {
    private final ResourceStorageApi fileStorage;
    private final ResourceMetadataServiceApi metadataService;
    private final StorageKeyResolverApi keyResolver;

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
        metadataService.markForDeletion(userId, path);
        fileStorage.delete(objectKey);
        metadataService.delete(userId, path);
    }

    private void deleteDirectory(Long userId, String path, String objectKey) {
        metadataService.markForDeletionByPrefix(userId, path);
        fileStorage.deleteByPrefix(objectKey);
        metadataService.deleteByPrefix(userId, path);
    }
}
