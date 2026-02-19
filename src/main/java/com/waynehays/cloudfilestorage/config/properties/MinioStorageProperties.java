package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio.storage")
public record MinioStorageProperties(

        @NotBlank(message = "Bucket name cannot be empty")
        @Length(max = 50, message = "Bucket name cannot be more than 50 symbols")
        String bucketName
) {
}
