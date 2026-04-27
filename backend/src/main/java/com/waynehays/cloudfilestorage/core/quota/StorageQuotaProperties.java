package com.waynehays.cloudfilestorage.core.quota;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage-quota")
record StorageQuotaProperties(

        @NotNull(message = "Default storage limit must be set")
        DataSize defaultLimit
) {
    private static final DataSize MIN_LIMIT = DataSize.ofMegabytes(100);
    private static final DataSize MAX_LIMIT = DataSize.ofGigabytes(10);

    @AssertTrue(message = "Default storage limit must be between 100 MB and 10 GB")
    boolean isDefaultLimitValid() {
        if (defaultLimit == null) {
            return true;
        }
        long bytes = defaultLimit.toBytes();
        return bytes >= MIN_LIMIT.toBytes() && bytes <= MAX_LIMIT.toBytes();
    }
}
