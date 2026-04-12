package com.waynehays.cloudfilestorage.integration.base;

import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class AbstractIntegrationBaseTest {
    protected static final String TEST_BUCKET_NAME = "test-bucket";

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8.6.0"))
            .withExposedPorts(6379);

    protected static final MinIOContainer MINIO =
            new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1"))
                    .withUserName("minioadmin")
                    .withPassword("minioadmin");

    private static MinioClient minioClient;

    static {
        try {
            POSTGRES.start();
            REDIS.start();
            MINIO.start();
            initMinioClientAndBucket();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start test containers", e);
        }
    }

    private static void initMinioClientAndBucket() {
        minioClient = MinioClient.builder()
                .endpoint(MINIO.getS3URL())
                .credentials(MINIO.getUserName(), MINIO.getPassword())
                .build();
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(TEST_BUCKET_NAME).build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test bucket", e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("minio.security.url", MINIO::getS3URL);
        registry.add("minio.security.access-key", MINIO::getUserName);
        registry.add("minio.security.secret-key", MINIO::getPassword);
        registry.add("minio.storage.bucket-name", () -> TEST_BUCKET_NAME);
    }

    protected static void cleanMinioBucket() {
        try {
            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(TEST_BUCKET_NAME).recursive(true).build()
            );
            List<DeleteObject> objects = new ArrayList<>();
            for (Result<Item> r : items) {
                objects.add(new DeleteObject(r.get().objectName()));
            }
            if (!objects.isEmpty()) {
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder().bucket(TEST_BUCKET_NAME).objects(objects).build()
                ).forEach(result -> {
                    try {
                        result.get();
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean MinIO bucket", e);
        }
    }
}
