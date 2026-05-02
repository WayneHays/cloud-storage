package com.waynehays.cloudfilestorage.files.api.resource.dto;

import com.waynehays.cloudfilestorage.infrastructure.validation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteRequest(

        @Schema(description = """
                Path to delete. Must contain only letters, digits, dots, dashes,
                underscores and spaces. Segments cannot start or end with a dot
                and cannot be '.' or '..'
                """,
                example = "docs/file.txt")
        @NotBlank(message = "Path to delete cannot be blank")
        @ValidPath
        String path
) {
}
