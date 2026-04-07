package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio.security")
public record MinioCredentialsProperties(

        @NotBlank(message = "URL MinIO must be set")
        @Pattern(regexp = "https?://.+", message = "URL must begin with https://")
        String url,

        @NotBlank(message = "Access key must be set")
        @Length(min = 5, max = 100, message = "Access key must be >= 5 and <= 100 characters")
        String accessKey,

        @NotBlank(message = "Secret key must be set")
        @Length(min = 5, max = 100, message = "Secrete key must be >= 5 and <= 100 characters")
        String secretKey
) {
}
