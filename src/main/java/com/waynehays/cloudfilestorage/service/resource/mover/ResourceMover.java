package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.constants.Messages;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceMover implements ResourceMoverApi {
    private static final String MSG_INVALID_MOVE = "Unable to move path to file";

    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        String objectKeyFrom = keyResolver.resolveKey(userId, pathFrom);
        String objectKeyTo = keyResolver.resolveKey(userId, pathTo);

        MetaData sourceMetaData = fileStorage.getMetaData(objectKeyFrom)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + pathFrom));

        validate(pathFrom, pathTo, objectKeyTo);

        if (PathUtils.isDirectory(pathFrom)) {
            moveDirectory(objectKeyFrom, objectKeyTo);
            return dtoConverter.directoryFromPath(pathTo);
        }

        fileStorage.move(objectKeyFrom, objectKeyTo);
        return dtoConverter.convert(sourceMetaData, pathTo);
    }

    private void validate(String pathFrom, String pathTo, String objectKeyTo) {
        if (PathUtils.isDirectory(pathFrom) && PathUtils.isFile(pathTo)) {
            throw new InvalidMoveException(MSG_INVALID_MOVE);
        }

        if (fileStorage.exists(objectKeyTo)) {
            throw new ResourceAlreadyExistsException(Messages.ALREADY_EXISTS + pathTo);
        }
    }

    private void moveDirectory(String objectKeyFrom, String objectKeyTo) {
        fileStorage.move(objectKeyFrom, objectKeyTo);

        fileStorage.getListRecursive(objectKeyFrom)
                .forEach(metaData -> {
                    String newObjectKey = metaData.key().replace(objectKeyFrom, objectKeyTo);
                    fileStorage.move(metaData.key(), newObjectKey);
                });
    }
}
