package com.waynehays.cloudfilestorage.resource.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "resource.limits")
public record ResourceLimitsProperties(

        @NotNull(message = "Max path length must be set")
        @Min(value = 1, message = "Max path length must be >= 1")
        @Max(value = 4096, message = "Max path length must be <= 4096")
        Integer maxPathLength,

        @NotNull(message = "Max filename length must be set")
        @Min(value = 1, message = "Max filename length must be >= 1")
        @Max(value = 255, message = "Max filename length must be <= 255")
        Integer maxFilenameLength,

        @NotNull(message = "Max file size must be set")
        DataSize maxFileSize
) {
    private static final DataSize MIN_FILE_SIZE = DataSize.ofMegabytes(1);
    private static final DataSize MAX_FILE_SIZE = DataSize.ofGigabytes(5);

    @AssertTrue(message = "Max file size must be between 1 MB and 5 GB")
    boolean isMaxFileSizeValid() {
        if (maxFileSize == null) {
            return true;
        }
        long bytes = maxFileSize.toBytes();
        return bytes >= MIN_FILE_SIZE.toBytes() && bytes <= MAX_FILE_SIZE.toBytes();
    }
}
