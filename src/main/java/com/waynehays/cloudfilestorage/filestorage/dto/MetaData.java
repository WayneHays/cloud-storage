package com.waynehays.cloudfilestorage.filestorage.dto;

import lombok.Builder;

@Builder
public record MetaData(
        String key,
        String name,
        Long size,
        String contentType,
        boolean isDirectory
) {
}
