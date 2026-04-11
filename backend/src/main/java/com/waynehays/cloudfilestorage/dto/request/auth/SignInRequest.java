package com.waynehays.cloudfilestorage.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignInRequest(

        @Schema(description = "Username, 3-50 characters, letters, digits, underscore only", example = "wayne_hays")
        @NotBlank(message = "Username must not be empty")
        @Pattern(regexp = "^\\w{3,50}$", message = "Username must be 3-50 characters, letters, digits, underscore only")
        String username,

        @Schema(description = "Password, 6-128 characters, letters, digits, underscore only", example = "password123")
        @NotBlank(message = "Password cannot be empty")
        @Pattern(regexp = "^\\w{6,128}$", message = "Password must be 6-128 characters, letters, digits, underscore only")
        String password
) {
}
