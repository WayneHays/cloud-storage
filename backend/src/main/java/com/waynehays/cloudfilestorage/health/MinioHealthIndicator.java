package com.waynehays.cloudfilestorage.health;

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
        String key = "bucket";
        String bucketName = properties.bucketName();

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                return Health.down()
                        .withDetail("bucket", bucketName)
                        .withDetail("reason", "bucket does not exist")
                        .build();
            }

            return Health.up()
                    .withDetail("bucket", bucketName)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail(key, bucketName)
                    .build();
        }
    }
}
