package com.waynehays.cloudfilestorage.core.metadata.dto;

public record FileRowDto(
        String storageKey,
        String path,
        String normalizedPath,
        String parentPath,
        String name,
        long size
) {}