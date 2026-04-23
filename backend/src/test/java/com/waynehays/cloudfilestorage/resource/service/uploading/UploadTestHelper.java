package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.FileRowDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.entity.ResourceType;
import com.waynehays.cloudfilestorage.shared.utils.PathUtils;

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
