package com.waynehays.cloudfilestorage.dto.internal;

public record NewDirectoryDto(
        String path,
        String parentPath,
        String name
) {
}
