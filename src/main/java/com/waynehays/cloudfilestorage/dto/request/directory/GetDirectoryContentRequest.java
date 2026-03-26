package com.waynehays.cloudfilestorage.dto.request.directory;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.Size;

public record GetDirectoryContentRequest(

        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
