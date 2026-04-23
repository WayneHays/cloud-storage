package com.waynehays.cloudfilestorage.resource.dto.request;

import com.waynehays.cloudfilestorage.shared.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(

        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
