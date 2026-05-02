package com.waynehays.cloudfilestorage.files.api.resource.dto;

import com.waynehays.cloudfilestorage.infrastructure.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(

        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
