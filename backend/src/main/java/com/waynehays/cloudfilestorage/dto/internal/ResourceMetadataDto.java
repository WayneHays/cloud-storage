package com.waynehays.cloudfilestorage.dto.internal;

import com.waynehays.cloudfilestorage.dto.ResourceType;

public record ResourceMetadataDto(
        Long id,
        Long userId,
        String path,
        String parentPath,
        String name,
        Long size,
        ResourceType type
) {
    public boolean isFile() {
        return type.isFile();
    }
}
