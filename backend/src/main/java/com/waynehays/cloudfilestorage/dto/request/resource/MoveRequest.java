package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.PathNotEquals;
import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;

@PathNotEquals
public record MoveRequest(

        @NotBlank(message = "Source path cannot be empty")
        @ValidPath(message = "Invalid source path")
        String from,

        @NotBlank(message = "Target path cannot be empty")
        @ValidPath(message = "Invalid target path")
        String to
) {
}
