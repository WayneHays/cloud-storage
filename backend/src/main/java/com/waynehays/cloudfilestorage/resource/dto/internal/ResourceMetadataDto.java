package com.waynehays.cloudfilestorage.resource.dto.internal;

import com.waynehays.cloudfilestorage.resource.entity.ResourceType;

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
