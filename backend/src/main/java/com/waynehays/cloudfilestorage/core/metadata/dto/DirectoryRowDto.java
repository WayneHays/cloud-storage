package com.waynehays.cloudfilestorage.core.metadata.dto;

public record DirectoryRowDto(
        String path,
        String normalizedPath,
        String parentPath,
        String name
) {}
