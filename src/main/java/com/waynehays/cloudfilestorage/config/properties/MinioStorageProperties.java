package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio.storage")
public record MinioStorageProperties(

        @NotBlank(message = "Bucket name cannot be empty")
        @Length(max = 50, message = "Bucket name cannot be more than 50 symbols")
        String bucketName,

        @NotBlank(message = "Batch size cannot be empty")
        @Size(min = 10, max = 1000, message = "Batch size must be > 10 and < 1000")
        Integer batchSize
) {
}
