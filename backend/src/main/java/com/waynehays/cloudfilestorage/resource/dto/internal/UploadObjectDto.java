package com.waynehays.cloudfilestorage.resource.dto.internal;

import com.waynehays.cloudfilestorage.storage.service.InputStreamSupplier;

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
