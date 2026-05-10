package com.waynehays.cloudfilestorage.files.api.dto.request;

import com.waynehays.cloudfilestorage.files.api.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(

        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
