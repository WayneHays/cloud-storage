package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MoveValidator {
    private final ResourceMetadataServiceApi metadataService;

    public void validate(Long userId, String pathFrom, String pathTo) {
        boolean isFromDirectory = PathUtils.isDirectory(pathFrom);

        if (isFromDirectory && PathUtils.isFile(pathTo)) {
            throwException("Cannot move directory to file", pathFrom, pathTo);
        }

        if (isFromDirectory && pathTo.startsWith(pathFrom)) {
            throwException("Cannot move directory into itself", pathFrom, pathTo);
        }

        metadataService.throwIfAnyExists(userId, List.of(pathTo));
    }

    private void throwException(String message, String pathFrom, String pathTo) {
        throw new InvalidMoveException(message, pathFrom, pathTo);
    }
}
