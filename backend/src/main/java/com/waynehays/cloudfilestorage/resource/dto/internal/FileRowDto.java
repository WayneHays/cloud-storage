package com.waynehays.cloudfilestorage.resource.dto.internal;

public record FileRowDto(
        String path,
        String normalizedPath,
        String parentPath,
        String name,
        long size
) {}