package com.waynehays.cloudfilestorage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User information")
public record UserDto(

        @Schema(description = "User ID",
                example = "1")
        Long id,

        @Schema(description = "Username",
                example = "wayne_hays")
        String username
) {
}
