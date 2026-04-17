package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record FileRowDto(
        String path,
        String parentPath,
        String name,
        long size
) {
}
