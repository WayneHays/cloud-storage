package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio.security")
public record MinioSecurityProperties(

        @NotBlank(message = "URL MinIO cannot be empty")
        @Pattern(regexp = "https?://.+", message = "URL must begin with http:// or https://")
        String url,

        @NotBlank(message = "Access key cannot be empty")
        String accessKey,

        @NotBlank(message = "Secret key cannot be empty")
        String secretKey
) {
}
