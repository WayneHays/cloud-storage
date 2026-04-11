package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;

public record UploadRequest(

        @Schema(description = """
                Path to directory where files will be uploaded. Empty for root or ends with '/'.
                Must contain only letters, digits, dots, dashes, underscores and spaces.
                Segments cannot start or end with a dot and cannot be '.' or '..'
                """,
                example = "docs/")
        @ValidPath(mustBeDirectory = true, message = "Path must be empty or end with /")
        String path
) {
}
