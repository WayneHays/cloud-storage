package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record DirectoryRowDto(
        String path,
        String normalizedPath,
        String parentPath,
        String name
) {}
