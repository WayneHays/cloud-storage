package com.waynehays.cloudfilestorage.infrastructure.errorhandling;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorDto(

        @Schema(description = "Error message describing what went wrong")
        String message
) {
}
