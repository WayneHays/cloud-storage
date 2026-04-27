package com.waynehays.cloudfilestorage.files.dto.internal;

import com.waynehays.cloudfilestorage.infrastructure.storage.InputStreamSupplier;

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
