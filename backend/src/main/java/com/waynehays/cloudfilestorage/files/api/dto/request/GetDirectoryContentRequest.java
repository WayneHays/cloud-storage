package com.waynehays.cloudfilestorage.files.api.dto.request;

import com.waynehays.cloudfilestorage.files.api.validation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;

public record GetDirectoryContentRequest(

        @Schema(description = """
                Path to directory to get content. Must end with '/', contain only letters, digits, dots, dashes,
                underscores and spaces, without '..' or '.' segments
                """,
                example = "docs/")
        @ValidPath(mustBeDirectory = true, message = "Invalid directory path")
        String path
) {
}
