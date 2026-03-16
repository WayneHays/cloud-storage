package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streaming")
public record ArchiveProperties(

        @NotBlank(message = "Buffer size must be set")
        @Size(min = 1024, max = 8192, message = "Buffer size must be more than 1024 and less than 8192")
        int bufferSize
) {
}
