package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record DeleteRequest(

        @NotBlank(message = "Cannot delete root path")
        @ValidPath
        String path
) {
}
