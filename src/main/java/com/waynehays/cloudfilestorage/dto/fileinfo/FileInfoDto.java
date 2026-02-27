package com.waynehays.cloudfilestorage.dto.fileinfo;

import lombok.Builder;

@Builder
public record FileInfoDto(
        Long id,
        String directory,
        String name,
        String storageKey,
        String contentType,
        Long size
) {
}
