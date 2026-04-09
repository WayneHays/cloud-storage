package com.waynehays.cloudfilestorage.dto.internal.metadata;

public record NewFileDto(
        String path,
        String parentPath,
        String name,
        long size
) {
}
