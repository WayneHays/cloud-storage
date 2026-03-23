package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
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
public class MinioBucketInitializer {
    private static final String MSG_INIT_FAILED = "Failed to initialize MinIO bucket: ";
    private static final String LOG_BUCKET_CREATED = "Created bucket: {}";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            if (!minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucketName())
                            .build())) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.bucketName())
                                .build()
                );
                log.info(LOG_BUCKET_CREATED, properties.bucketName());
            }
        } catch (Exception e) {
            throw new FileStorageException(MSG_INIT_FAILED + properties.bucketName());
        }
    }
}
