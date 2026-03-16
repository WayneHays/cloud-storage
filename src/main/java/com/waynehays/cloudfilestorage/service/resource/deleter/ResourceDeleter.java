package com.waynehays.cloudfilestorage.service.resource.deleter;

import com.waynehays.cloudfilestorage.constants.Messages;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceDeleter implements ResourceDeleterApi {
    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;

    @Override
    public void delete(Long userId, String path) {
        String objectKey = keyResolver.resolveKey(userId, path);

        if (!fileStorage.exists(objectKey)) {
            throw new ResourceNotFoundException(Messages.NOT_FOUND + path);
        }

        if (PathUtils.isFile(path)) {
            fileStorage.delete(objectKey);
        } else {
            fileStorage.deleteByPrefix(objectKey);
        }
    }
}
