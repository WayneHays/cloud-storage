package com.waynehays.cloudfilestorage.dto.files.response;

import java.io.InputStream;

public record FileDownloadDto(
        InputStream inputStream,
        long size
) {
}
