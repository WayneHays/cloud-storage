package com.waynehays.cloudfilestorage.dto.response;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadResult(
        StreamingResponseBody body,
        String name,
        ResourceType type
) {
}
