package com.waynehays.cloudfilestorage.files.dto.request.directory;

import com.waynehays.cloudfilestorage.infrastructure.path.ValidPath;
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
