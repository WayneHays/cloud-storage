package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ValidateMoveStep implements MoveStep {
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(MoveContext context) {
        String pathFrom = context.getPathFrom();
        String pathTo = context.getPathTo();

        if (PathUtils.isDirectory(pathFrom)) {
            if (PathUtils.isFile(pathTo)) {
                throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
            }
            if (pathTo.startsWith(pathFrom)) {
                throw new InvalidMoveException("Cannot move directory into itself", pathFrom, pathTo);
            }
        }

        if (metadataService.existsByPath(context.getUserId(), pathTo)) {
            throw new ResourceAlreadyExistsException("Resource already exists at target path", pathTo);
        }
    }
}
