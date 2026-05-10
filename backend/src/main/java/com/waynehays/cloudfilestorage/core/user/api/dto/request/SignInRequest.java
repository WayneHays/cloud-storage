package com.waynehays.cloudfilestorage.core.user.api.dto.request;

import com.waynehays.cloudfilestorage.core.user.api.validation.ValidPassword;
import com.waynehays.cloudfilestorage.core.user.api.validation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;

public record SignInRequest(

        @Schema(description = "Username, 3-50 characters, letters, digits, underscore only", example = "wayne_hays")
        @ValidUsername
        String username,

        @Schema(description = "Password, 6-128 characters, letters, digits, underscore only", example = "password123")
        @ValidPassword
        String password
) {
}
