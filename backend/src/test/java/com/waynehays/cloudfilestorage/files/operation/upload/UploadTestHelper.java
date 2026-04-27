package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;

import java.io.InputStream;
import java.util.List;

class UploadTestHelper {

    static UploadContext uploadContext(Long userId, UploadObjectDto... objects) {
        return new UploadContext(userId, List.of(objects));
    }

    static UploadObjectDto uploadObject(String fullPath, long size) {
        return new UploadObjectDto(
                "file.txt",
                "file.txt",
                PathUtils.extractParentPath(fullPath),
                fullPath,
                size,
                "application/octet-stream",
                InputStream::nullInputStream
        );
    }

    static ResourceDto fileDto(String path, String name, long size) {
        return new ResourceDto(path, name, size, ResourceType.FILE);
    }

    static ResourceDto dirDto(String path, String name) {
        return new ResourceDto(path, name, null, ResourceType.DIRECTORY);
    }

    static FileRowDto fileRowDto(String path, String name) {
        return new FileRowDto(
                path,
                path.toLowerCase(),
                PathUtils.extractParentPath(path),
                name,
                0L
        );
    }

    static DirectoryRowDto dirRowDto(String path, String name) {
        return new DirectoryRowDto(
                path,
                path.toLowerCase(),
                PathUtils.extractParentPath(path),
                name
        );
    }
}
