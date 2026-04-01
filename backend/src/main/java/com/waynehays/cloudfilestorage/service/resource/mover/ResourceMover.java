package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
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
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, pathFrom);
        validatePaths(userId, pathFrom, pathTo);

        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);

        if (PathUtils.isDirectory(pathFrom)) {
            moveDirectory(userId, pathFrom, pathTo);
            return dtoConverter.directoryFromPath(pathTo);
        }

        moveFile(userId, pathFrom, pathTo, keyFrom, keyTo);
        return dtoConverter.fileFromPath(pathTo, dto.size());
    }

    private void validatePaths(Long userId, String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom) && PathUtils.isFile(pathTo)) {
            throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
        }
        metadataService.throwIfAnyExists(userId, List.of(pathTo));
    }

    private void moveFile(Long userId, String pathFrom, String pathTo, String keyFrom, String keyTo) {
        log.info("Start move file: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        resourceStorage.moveObject(keyFrom, keyTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo) {
        log.info("Start move directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        List<ResourceMetadataDto> content = metadataService.findDirectoryContent(userId, pathFrom);
        moveContent(userId, content, pathFrom, pathTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveContent(Long userId, List<ResourceMetadataDto> content, String pathFrom, String pathTo) {
        content.stream()
                .filter(ResourceMetadataDto::isFile)
                .forEach(dto -> {
                    String oldKey = keyResolver.resolveKey(userId, dto.path());
                    String newPath = dto.path().replace(pathFrom, pathTo);
                    String newKey = keyResolver.resolveKey(userId, newPath);
                    resourceStorage.moveObject(oldKey, newKey);
                });
    }
}
