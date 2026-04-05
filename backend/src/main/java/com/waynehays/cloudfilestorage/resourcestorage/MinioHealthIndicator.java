package com.waynehays.cloudfilestorage.resourcestorage;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioHealthIndicator implements HealthIndicator {
    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @Override
    public @Nullable Health health() {
        try {
            minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucketName())
                            .build()
            );
            return Health.up()
                    .withDetail("bucket", properties.bucketName())
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("bucket", properties.bucketName())
                    .build();
        }
    }
}
