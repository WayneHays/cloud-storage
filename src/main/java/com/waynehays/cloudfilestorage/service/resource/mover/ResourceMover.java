package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.constant.Messages;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceMover implements ResourceMoverApi {
    private static final String MSG_INVALID_MOVE = "Unable to move path to file";

    private final ResourceStorageApi fileStorage;
    private final ResourceMetadataServiceApi metadataService;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        ResourceMetadata sourceMetadata = metadataService.findOrThrow(userId, pathFrom);
        validate(userId, pathFrom, pathTo);

        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);

        if (PathUtils.isDirectory(pathFrom)) {
            moveDirectory(userId, pathFrom, pathTo, keyFrom, keyTo);
            return dtoConverter.directoryFromPath(pathTo);
        }

        moveFile(userId, pathFrom, pathTo, keyFrom, keyTo);
        return dtoConverter.fileFromPath(pathTo, sourceMetadata.getSize());
    }

    private void validate(Long userId, String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom) && PathUtils.isFile(pathTo)) {
            throw new InvalidMoveException(MSG_INVALID_MOVE);
        }

        if (metadataService.exists(userId, pathTo)) {
            throw new ResourceAlreadyExistsException(Messages.ALREADY_EXISTS + pathTo);
        }
    }

    private void moveFile(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        fileStorage.move(keyFrom, keyTo);
        metadataService.updatePath(userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        List<ResourceMetadata> content = metadataService.findDirectoryContent(userId, pathFrom);

        metadataService.markForDeletionByPrefix(userId, pathFrom);

        fileStorage.createDirectory(keyTo);
        fileStorage.delete(keyFrom);

        content.forEach(metadata -> {
            String oldKey = keyResolver.resolveKey(userId, metadata.getPath());
            String newPath = metadata.getPath().replace(pathFrom, pathTo);
            String newKey = keyResolver.resolveKey(userId, newPath);

            if (metadata.isFile()) {
                fileStorage.move(oldKey, newKey);
            } else {
                fileStorage.createDirectory(newKey);
                fileStorage.delete(oldKey);
            }
        });

        metadataService.updateContentPaths(content, pathFrom, pathTo);
    }
}
