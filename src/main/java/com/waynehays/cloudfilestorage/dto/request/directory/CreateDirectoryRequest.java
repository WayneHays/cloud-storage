package com.waynehays.cloudfilestorage.dto.request.directory;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDirectoryRequest(
        @NotBlank(message = "Directory path cannot be blank")
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
