package com.waynehays.cloudfilestorage.integration.container;

import com.waynehays.cloudfilestorage.storage.minio.config.MinioStorageProperties;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.MinIOContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class MinioTestContainer {
    private static final String BUCKET = "test-bucket";
    private static final String MINIO_VERSION = "minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1";
    private static final String USERNAME = "minioadmin";
    private static final String PASSWORD = "minioadmin";
    private static final MinIOContainer CONTAINER;
    private static final MinioClient CLIENT;
    private static final MinioStorageProperties PROPERTIES;

    static {
        try {
            CONTAINER = createContainer();
            CONTAINER.start();

            CLIENT = createClient();
            PROPERTIES = createProperties();

            createTestBucket();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO test container");
        }
    }

    @SuppressWarnings("resource")
    private static MinIOContainer createContainer() {
        return new MinIOContainer(MINIO_VERSION)
                .withUserName(USERNAME)
                .withPassword(PASSWORD);
    }

    public static String getUrl() {
        return CONTAINER.getS3URL();
    }

    public static String getUsername() {
        return CONTAINER.getUserName();
    }

    public static String getPassword() {
        return CONTAINER.getPassword();
    }

    public static MinioClient getClient() {
        return CLIENT;
    }

    public static MinioStorageProperties getProperties() {
        return PROPERTIES;
    }

    public static void cleanTestBucket() {
        try {
            Iterable<Result<Item>> items = CLIENT.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(BUCKET)
                            .recursive(true)
                            .build()
            );
            List<DeleteObject> objects = new ArrayList<>();

            for (Result<Item> r : items) {
                objects.add(new DeleteObject(r.get().objectName()));
            }

            if (!objects.isEmpty()) {
                CLIENT.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(BUCKET)
                                .objects(objects)
                                .build()
                ).forEach(result -> {
                    try {
                        result.get();
                    } catch (Exception e) {
                        log.warn("Failed to delete object during test cleanup", e);
                    }
                });
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clean MinIO bucket", e);
        }
    }

    public static void removeTestBucket() {
        try {
            CLIENT.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(BUCKET)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove test bucket after tests", e);
        }
    }

    private static MinioStorageProperties createProperties() {
        return new MinioStorageProperties(
                BUCKET,
                2,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10));
    }

    private static MinioClient createClient() {
        return MinioClient.builder()
                .endpoint(getUrl())
                .credentials(CONTAINER.getUserName(), CONTAINER.getPassword())
                .build();
    }

    private static void createTestBucket() {
        try {
            CLIENT.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(BUCKET)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test bucket", e);
        }
    }

    private MinioTestContainer() {
    }
}
