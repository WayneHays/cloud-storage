package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record MoveRequest(

        @NotBlank(message = "Source path cannot be empty")
        @ValidPath(message = "Invalid source path")
        String from,

        @NotBlank(message = "Target path cannot be empty")
        @ValidPath(message = "Invalid target path")
        String to
) {
    @AssertTrue(message = "Source and target paths must be different")
    boolean isPathsNotEqual() {
        if (from == null || to == null) {
            return true;
        }
        return !from.equalsIgnoreCase(to);
    }
}
