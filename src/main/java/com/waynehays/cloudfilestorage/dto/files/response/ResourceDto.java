package com.waynehays.cloudfilestorage.dto.files.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ResourceDto(
        String path,
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long size,
        ResourceType type
) {
    public static ResourceDto file(String path, String name, long size) {
        return new ResourceDto(path, name, size, ResourceType.FILE);
    }

    public static ResourceDto directory(String path, String name) {
        return new ResourceDto(path, name, null, ResourceType.DIRECTORY);
    }
}
