package com.waynehays.cloudfilestorage.infrastructure.storage.minio;

import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class MinioBucketInitializer {
    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void createBucketIfNotExists() {
        String bucketName = properties.bucketName();
        try {
            if (!minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build())) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName);
            throw new ResourceStorageException("Failed to initialize MinIO bucket: " + bucketName);
        }
    }
}
