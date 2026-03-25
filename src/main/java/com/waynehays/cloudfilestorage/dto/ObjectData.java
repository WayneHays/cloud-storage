package com.waynehays.cloudfilestorage.dto;

public record ObjectData(
        String originalFilename,
        String filename,
        String directory,
        String fullPath,
        long size,
        String contentType,
        InputStreamSupplier inputStreamSupplier
) {
}
