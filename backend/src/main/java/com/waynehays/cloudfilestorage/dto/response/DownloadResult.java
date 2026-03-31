package com.waynehays.cloudfilestorage.dto.response;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadResult(
        StreamingResponseBody body,
        String name,
        String contentType
) {
}
