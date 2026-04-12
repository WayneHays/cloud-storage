package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record FileRow(
        String path,
        String parentPath,
        String name,
        long size
) {
}
