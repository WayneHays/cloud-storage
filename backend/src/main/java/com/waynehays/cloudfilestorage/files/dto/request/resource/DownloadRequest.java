package com.waynehays.cloudfilestorage.files.dto.request.resource;

import com.waynehays.cloudfilestorage.infrastructure.path.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(

        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
