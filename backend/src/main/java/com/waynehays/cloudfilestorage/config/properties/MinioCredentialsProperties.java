package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio.security")
public record MinioCredentialsProperties(

        @NotBlank(message = "URL MinIO must be set")
        @Pattern(regexp = "https?://.+", message = "URL must begin with http:// or https://")
        String url,

        @NotBlank(message = "Access key must be set")
        String accessKey,

        @NotBlank(message = "Secret key must be set")
        String secretKey
) {
}
