package com.waynehays.cloudfilestorage.core.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponse(

        @Schema(description = "Error message describing what went wrong")
        String message
) {
}
