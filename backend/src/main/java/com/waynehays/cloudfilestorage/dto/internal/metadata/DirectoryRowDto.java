package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record DirectoryRowDto(
        String path,
        String parentPath,
        String name
) {
}
