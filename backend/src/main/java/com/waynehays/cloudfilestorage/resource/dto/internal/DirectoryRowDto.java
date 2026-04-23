package com.waynehays.cloudfilestorage.resource.dto.internal;

public record DirectoryRowDto(
        String path,
        String normalizedPath,
        String parentPath,
        String name
) {}
