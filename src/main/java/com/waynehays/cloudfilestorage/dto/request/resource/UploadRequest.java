package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UploadRequest(
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @Pattern(regexp = "^$|^[a-zA-Z0-9._-]+(/[a-zA-Z0-9._-]+)*/?$",
                message = "Path contains invalid characters or format")
        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
