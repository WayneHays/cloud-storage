package com.waynehays.cloudfilestorage.dto;

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
