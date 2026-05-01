package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceMoveService implements ResourceMoveServiceApi {
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoMapper mapper;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        log.info("Move started: from={}, to={}", pathFrom, pathTo);

        validate(userId, pathFrom, pathTo);
        metadataService.moveMetadata(userId, pathFrom, pathTo);

        log.info("Move completed: from={}, to={}", pathFrom, pathTo);

        if (PathUtils.isFile(pathTo)) {
            ResourceMetadataDto moved = metadataService.findByPath(userId, pathTo);
            return mapper.fileFromPath(pathTo, moved.size());
        }
        return mapper.directoryFromPath(pathTo);
    }

    private void validate(Long userId, String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom)) {
            if (PathUtils.isFile(pathTo)) {
                throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
            }
            if (pathTo.startsWith(pathFrom)) {
                throw new InvalidMoveException("Cannot move directory into itself", pathFrom, pathTo);
            }
        }

        String targetParent = PathUtils.extractParentPath(pathTo);

        if (!targetParent.isEmpty() && !metadataService.existsByPath(userId, targetParent)) {
            throw new ResourceNotFoundException("Target directory not found", targetParent);
        }

        if (metadataService.existsByPath(userId, pathTo)) {
            throw new ResourceAlreadyExistsException("Resource already exists at target path", pathTo);
        }

        metadataService.throwIfAnyConflictingTypeExists(userId, List.of(pathTo));
    }
}
