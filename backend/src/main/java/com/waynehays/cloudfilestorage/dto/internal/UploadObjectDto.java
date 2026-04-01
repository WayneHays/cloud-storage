package com.waynehays.cloudfilestorage.dto.internal;

import com.waynehays.cloudfilestorage.dto.InputStreamSupplier;

public record UploadObjectDto(
        String originalFilename,
        String filename,
        String directory,
        String fullPath,
        long size,
        String contentType,
        InputStreamSupplier inputStreamSupplier
) {
}
