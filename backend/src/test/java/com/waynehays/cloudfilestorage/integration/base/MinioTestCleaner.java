package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioTestCleaner {
    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public void deleteAll() {
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.bucketName())
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : objects) {
            try {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(item.objectName())
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to clean up bucket", e);
            }
        }
    }
}
