package com.waynehays.cloudfilestorage.resource.dto.request;

import com.waynehays.cloudfilestorage.shared.annotation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GetInfoRequest(

        @Schema(description = """
                Path to get info. Must contain only letters, digits, dots, dashes,
                underscores and spaces. Segments cannot start or end with a dot
                and cannot be '.' or '..'
                """,
                example = "docs/file.txt")
        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
