package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.storage")
public record UserStorageLimitProperties(

        @NotNull(message = "Default storage limit must be set")
        @Min(value = 104857600, message = "Default storage limit must be >= 1 MB")
        @Max(value = 10737418240L, message = "Default storage limit must be <= 100 MB")
        Long defaultLimitBytes
) {
}
