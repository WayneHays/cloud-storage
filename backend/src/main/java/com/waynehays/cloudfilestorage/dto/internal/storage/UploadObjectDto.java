package com.waynehays.cloudfilestorage.dto.internal.storage;

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
