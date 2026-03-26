package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.PathNotEquals;
import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PathNotEquals
public record MoveRequest(

        @NotBlank(message = "Source path cannot be empty")
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath(message = "Invalid source path")
        String from,

        @NotBlank(message = "Target path cannot be empty")
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath(message = "Invalid target path")
        String to
) {
}
