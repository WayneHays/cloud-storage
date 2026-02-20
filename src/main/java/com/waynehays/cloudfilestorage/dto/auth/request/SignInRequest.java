package com.waynehays.cloudfilestorage.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignInRequest(

        @NotBlank(message = "Username must not be empty")
        @Pattern(regexp = "^\\w{3,50}$", message = "Username must be 3-50 characters, letters, digits, underscore only")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 128, message = "Password must be at least 6 and max 128 symbols")
        String password
) {
}
