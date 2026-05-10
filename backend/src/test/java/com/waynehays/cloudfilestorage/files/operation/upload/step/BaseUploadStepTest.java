package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;

import java.io.InputStream;
import java.util.List;

abstract class BaseUploadStepTest {
    protected static final Long USER_ID = 1L;

    Context uploadContext(UploadObjectDto... objects) {
        return new Context(USER_ID, List.of(objects));
    }

    UploadObjectDto uploadObject(String storageKey, String fullPath, long size) {
        return new UploadObjectDto(
                storageKey,
                "file.txt",
                "file.txt",
                PathUtils.getParentPath(fullPath),
                fullPath,
                size,
                "application/octet-stream",
                InputStream::nullInputStream
        );
    }

    ResourceResponse fileDto(String path, String name, long size) {
        return new ResourceResponse(path, name, size, ResourceType.FILE);
    }

    ResourceResponse directoryDto(String path, String name) {
        return new ResourceResponse(path, name, null, ResourceType.DIRECTORY);
    }
}