package com.waynehays.cloudfilestorage.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignInRequest(

        @NotBlank(message = "Username must not be empty")
        @Pattern(regexp = "^\\w{3,50}$", message = "Username must be 3-50 characters, letters, digits, underscore only")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Pattern(regexp = "^\\w{6,128}$", message = "Password must be 6-128 characters, letters, digits, underscore only")
        String password
) {
}
