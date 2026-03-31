package com.waynehays.cloudfilestorage.dto.request.directory;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record CreateDirectoryRequest(

        @NotBlank(message = "Directory path cannot be blank")
        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
