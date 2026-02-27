package com.waynehays.cloudfilestorage.dto.file.response;

import java.io.InputStream;

public record FileDownloadDto(
        InputStream inputStream,
        long size,
        String filename
) {
}
