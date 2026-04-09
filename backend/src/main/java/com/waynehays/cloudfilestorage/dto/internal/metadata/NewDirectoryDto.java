package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record NewDirectoryDto(
        String path,
        String parentPath,
        String name
) {
}
