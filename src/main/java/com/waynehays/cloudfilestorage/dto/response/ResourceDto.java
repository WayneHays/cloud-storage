package com.waynehays.cloudfilestorage.dto.response;

import com.waynehays.cloudfilestorage.dto.ResourceType;

public record ResourceDto(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
