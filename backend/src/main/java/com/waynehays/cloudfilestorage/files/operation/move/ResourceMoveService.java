package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.move.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceMoveService implements ResourceMoveServiceApi {
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceResponseMapper responseMapper;

    @Override
    @Transactional
    public ResourceResponse move(Long userId, String pathFrom, String pathTo) {
        log.info("Move started: from={}, to={}", pathFrom, pathTo);

        validate(userId, pathFrom, pathTo);
        metadataService.moveMetadata(userId, pathFrom, pathTo);
        ResourceMetadataDto moved = metadataService.findByPath(userId, pathTo);

        log.info("Move completed: from={}, to={}", pathFrom, pathTo);
        return responseMapper.fromResourceMetadataDto(moved);
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

        String targetParent = PathUtils.getParentPath(pathTo);

        if (!targetParent.isEmpty() && !metadataService.existsByPath(userId, targetParent)) {
            throw new ResourceNotFoundException("Target directory not found", targetParent);
        }

        if (metadataService.existsByPath(userId, pathTo)) {
            throw new ResourceAlreadyExistsException("Resource already exists at target path", pathTo);
        }

        metadataService.throwIfAnyConflictingTypeExists(userId, List.of(pathTo));
    }
}
