package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.entity.ResourceType;
import com.waynehays.cloudfilestorage.shared.utils.PathUtils;

class MoveTestHelper {

    static final Long USER_ID = 1L;

    static MoveContext fileContext(String pathFrom, String pathTo) {
        return new MoveContext(USER_ID, pathFrom, pathTo, fileMetadata(1L, pathFrom, 100L));
    }

    static MoveContext directoryContext(String pathFrom, String pathTo) {
        return new MoveContext(USER_ID, pathFrom, pathTo, dirMetadata(1L, pathFrom));
    }

    static ResourceMetadataDto fileMetadata(Long id, String path, Long size) {
        return new ResourceMetadataDto(id, USER_ID, path,
                PathUtils.extractParentPath(path), PathUtils.extractFilename(path),
                size, ResourceType.FILE);
    }

    static ResourceMetadataDto dirMetadata(Long id, String path) {
        return new ResourceMetadataDto(id, USER_ID, path,
                PathUtils.extractParentPath(path), PathUtils.extractFilename(path),
                null, ResourceType.DIRECTORY);
    }
}
