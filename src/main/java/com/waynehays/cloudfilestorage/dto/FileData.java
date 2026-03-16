package com.waynehays.cloudfilestorage.dto;

import lombok.Builder;

@Builder
public record FileData(
        String originalFilename,
        String filename,
        String directory,
        String fullPath,
        long size,
        String contentType,
        InputStreamSupplier inputStreamSupplier
) {
}
