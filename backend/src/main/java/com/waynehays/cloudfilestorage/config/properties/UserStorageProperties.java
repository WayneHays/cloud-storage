package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "user.storage")
public record UserStorageProperties(

        @NotNull(message = "Default storage limit must be set")
        DataSize defaultLimit,

        @NotNull(message = "Reconciliation interval must be set")
        Duration reconciliationInterval
) {
}
