package com.waynehays.cloudfilestorage.integration.config.initializer;

import com.waynehays.cloudfilestorage.integration.config.container.MinioTestContainerInitializer;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

public class MinioInitializer {

    public static void createBucket(String bucketName) {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(MinioTestContainerInitializer.getUrl())
                    .credentials(MinioTestContainerInitializer.getUser(), MinioTestContainerInitializer.getPassword())
                    .build();

            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bucket: " + bucketName, e);
        }
    }
}
