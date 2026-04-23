package com.waynehays.cloudfilestorage.resource.dto.request;

import com.waynehays.cloudfilestorage.shared.annotation.ValidPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record MoveRequest(

        @Schema(description = """
                Path to resource before move. Must contain only letters, digits, dots, dashes,
                underscores and spaces. Segments cannot start or end with a dot
                and cannot be '.' or '..'
                """,
                example = "docs/file.txt")
        @NotBlank(message = "Source path cannot be empty")
        @ValidPath(message = "Invalid source path")
        String from,

        @Schema(description = """
                Path to resource after move. Must contain only letters, digits, dots, dashes,
                underscores and spaces. Segments cannot start or end with a dot
                and cannot be '.' or '..'
                """,
                example = "docs/work/file.txt")
        @NotBlank(message = "Target path cannot be empty")
        @ValidPath(message = "Invalid target path")
        String to
) {
    @AssertTrue(message = "Source and target paths must be different")
    boolean isPathsDifferent() {
        if (from == null || to == null) {
            return true;
        }
        return !from.equalsIgnoreCase(to);
    }
}
