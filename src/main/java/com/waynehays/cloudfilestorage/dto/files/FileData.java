package com.waynehays.cloudfilestorage.dto.files;

import lombok.Builder;

import java.io.InputStream;

@Builder
public record FileData(
        String originalFilename,
        String filename,
        String directory,
        String extension,
        long size,
        String contentType,
        InputStream inputStream
) {
}
