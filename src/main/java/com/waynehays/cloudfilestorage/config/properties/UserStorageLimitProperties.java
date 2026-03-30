package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "user.storage")
public record UserStorageLimitProperties(

        @NotNull(message = "Default storage limit must be set")
        DataSize defaultLimit
) {
}
