package com.waynehays.cloudfilestorage.core.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User information")
public record UserDto(

        @Schema(description = "Username",
                example = "wayne_hays")
        String username
) {
}
