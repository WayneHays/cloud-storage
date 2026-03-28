package com.waynehays.cloudfilestorage.service.ratelimit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RateLimitRule(

        @NotBlank(message = "Endpoint must not be blank")
        @Size(min = 1, max = 256, message = "Endpoint length must be between 1 and 256 characters")
        String endpoint,

        @NotBlank(message = "HTTP method must not be blank")
        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD",
                message = "HTTP method must be one of: GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD")
        @Size(max = 10, message = "HTTP method too long")
        String httpMethod,

        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 1000, message = "Capacity must not exceed 1000 tokens")
        int capacity,

        @Min(value = 1, message = "Tokens per minute must be at least 1")
        @Max(value = 6000, message = "Tokens per minute must not exceed 6000")
        int tokensPerMinute
) {
}
