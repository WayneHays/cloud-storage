package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMover implements ResourceMoverApi {
    private final ResourceStorageApi resourceStorage;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceDtoConverter dtoConverter;
    private final ResourceMetadataServiceApi metadataService;

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
            throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
        }
        metadataService.throwIfExists(userId, pathTo);
    }

    private void moveFile(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        log.info("Start move file: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        resourceStorage.moveObject(keyFrom, keyTo);
        metadataService.updatePath(userId, pathFrom, pathTo);

        log.info("Successfully moved file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        log.info("Start move directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        List<ResourceMetadata> content = metadataService.findDirectoryContent(userId, pathFrom);
        metadataService.markForDeletionByPrefix(userId, pathFrom);

        recreateDirectoryMarker(keyFrom, keyTo);
        moveContent(userId, content, pathFrom, pathTo);

        metadataService.batchUpdatePaths(content, pathFrom, pathTo);

        log.info("Successfully moved directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void recreateDirectoryMarker(String oldKey, String newKey) {
        resourceStorage.createDirectory(newKey);
        resourceStorage.deleteObject(oldKey);
    }

    private void moveContent(Long userId, List<ResourceMetadata> resourcesToMove, String pathFrom, String pathTo) {
        resourcesToMove.forEach(metadata -> {
            String oldKey = keyResolver.resolveKey(userId, metadata.getPath());
            String newPath = metadata.getPath().replace(pathFrom, pathTo);
            String newKey = keyResolver.resolveKey(userId, newPath);

            if (metadata.isFile()) {
                resourceStorage.moveObject(oldKey, newKey);
            } else {
                recreateDirectoryMarker(oldKey, newKey);
            }
        });
    }
}
