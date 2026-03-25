package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMover implements ResourceMoverApi {
    private static final String MSG_INVALID_MOVE = "Cannot move directory to file: userId=%d, '%s' -> '%s'";
    private static final String LOG_START_MOVE_FILE = "Start move file: userId={}, from={}, to={}";
    private static final String LOG_START_MOVE_DIRECTORY = "Start moving directory: userId={}, from={}, to={}";
    private static final String LOG_SUCCESS_MOVE_FILE = "Successfully moved file: userId={}, from={}, to={}";
    private static final String LOG_SUCCESS_MOVE_DIRECTORY = "Successfully moved directory: userId={}, from={}, to={}";

    private final ResourceStorageApi fileStorage;
    private final ResourceMetadataServiceApi metadataService;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        ResourceMetadata sourceMetadata = metadataService.findOrThrow(userId, pathFrom);
        validatePaths(userId, pathFrom, pathTo);

        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);

        if (PathUtils.isDirectory(pathFrom)) {
            moveDirectory(userId, pathFrom, pathTo, keyFrom, keyTo);
            return dtoConverter.directoryFromPath(pathTo);
        }

        moveFile(userId, pathFrom, pathTo, keyFrom, keyTo);
        return dtoConverter.fileFromPath(pathTo, sourceMetadata.getSize());
    }

    private void validatePaths(Long userId, String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom) && PathUtils.isFile(pathTo)) {
            throw new InvalidMoveException(MSG_INVALID_MOVE.formatted(userId, pathFrom, pathTo));
        }
        metadataService.throwIfExists(userId, pathTo);
    }

    private void moveFile(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        log.info(LOG_START_MOVE_FILE, userId, pathFrom, pathTo);

        fileStorage.moveObject(keyFrom, keyTo);
        metadataService.updatePath(userId, pathFrom, pathTo);

        log.info(LOG_SUCCESS_MOVE_FILE, userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        log.info(LOG_START_MOVE_DIRECTORY, userId, pathFrom, pathTo);

        List<ResourceMetadata> content = metadataService.findDirectoryContent(userId, pathFrom);
        metadataService.markForDeletionByPrefix(userId, pathFrom);

        recreateDirectoryMarker(keyFrom, keyTo);
        moveContent(userId, content, pathFrom, pathTo);

        metadataService.batchUpdatePaths(content, pathFrom, pathTo);

        log.info(LOG_SUCCESS_MOVE_DIRECTORY, userId, pathFrom, pathTo);
    }

    private void recreateDirectoryMarker(String oldKey, String newKey) {
        fileStorage.createDirectory(newKey);
        fileStorage.deleteObject(oldKey);
    }

    private void moveContent(Long userId, List<ResourceMetadata> resourcesToMove, String pathFrom, String pathTo) {
        resourcesToMove.forEach(metadata -> {
            String oldKey = keyResolver.resolveKey(userId, metadata.getPath());
            String newPath = metadata.getPath().replace(pathFrom, pathTo);
            String newKey = keyResolver.resolveKey(userId, newPath);

            if (metadata.isFile()) {
                fileStorage.moveObject(oldKey, newKey);
            } else {
                recreateDirectoryMarker(oldKey, newKey);
            }
        });
    }
}
