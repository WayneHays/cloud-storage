package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record DirectoryRow(
        String path,
        String parentPath,
        String name
) {
}
