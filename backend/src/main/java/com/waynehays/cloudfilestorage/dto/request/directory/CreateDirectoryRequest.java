package com.waynehays.cloudfilestorage.dto.request.directory;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateDirectoryRequest(

        @Schema(description = """
                Path to directory to create. Must end with '/', contain only letters, digits, dots, dashes,
                underscores and spaces, without '..' or '.' segments
                """,
                example = "docs/")
        @NotBlank(message = "Directory path cannot be blank")
        @ValidPath(mustBeDirectory = true, message = "Invalid directory path")
        String path
) {
}
