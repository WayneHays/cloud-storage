package com.waynehays.cloudfilestorage.files.operation.upload.dto;

import org.springframework.core.io.InputStreamSource;

public record UploadObjectDto(
        String storageKey,
        String originalFilename,
        String filename,
        String directory,
        String fullPath,
        long size,
        String contentType,
        InputStreamSource inputStreamSource
) {
}
