package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteRequest(
        @NotBlank(message = "Cannot delete root path")
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath
        String path
) {
}
