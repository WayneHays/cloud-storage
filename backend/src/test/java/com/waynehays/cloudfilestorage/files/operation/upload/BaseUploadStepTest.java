package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;

import java.io.InputStream;
import java.util.List;

abstract class BaseUploadStepTest {
    protected static final Long USER_ID = 1L;

    UploadContext uploadContext(UploadObjectDto... objects) {
        return new UploadContext(USER_ID, List.of(objects));
    }

    UploadObjectDto uploadObject(String storageKey, String fullPath, long size) {
        return new UploadObjectDto(
                storageKey,
                "file.txt",
                "file.txt",
                PathUtils.extractParentPath(fullPath),
                fullPath,
                size,
                "application/octet-stream",
                InputStream::nullInputStream
        );
    }

    ResourceDto fileDto(String path, String name, long size) {
        return new ResourceDto(path, name, size, ResourceType.FILE);
    }

    ResourceDto directoryDto(String path, String name) {
        return new ResourceDto(path, name, null, ResourceType.DIRECTORY);
    }

    FileRowDto fileRowDto(String storageKey, String path, String name) {
        return new FileRowDto(
                storageKey,
                path,
                path.toLowerCase(),
                PathUtils.extractParentPath(path),
                name,
                0L
        );
    }

    DirectoryRowDto directoryRowDto(String path, String name) {
        return new DirectoryRowDto(
                path,
                path.toLowerCase(),
                PathUtils.extractParentPath(path),
                name
        );
    }
}
