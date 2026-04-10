package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.integration.config.MinioInitializer;
import com.waynehays.cloudfilestorage.integration.config.MinioTestContainerInitializer;
import com.waynehays.cloudfilestorage.integration.config.PostgresTestContainerInitializer;
import com.waynehays.cloudfilestorage.integration.config.RedisTestContainerInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationBaseTest {
    private static final String TEST_BUCKET_NAME = "test-bucket";

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerInitializer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerInitializer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerInitializer::getPassword);
        registry.add("spring.data.redis.host", RedisTestContainerInitializer::getHost);
        registry.add("spring.data.redis.port", RedisTestContainerInitializer::getPort);
        registry.add("minio.security.url", MinioTestContainerInitializer::getUrl);
        registry.add("minio.security.access-key", MinioTestContainerInitializer::getUser);
        registry.add("minio.security.secret-key", MinioTestContainerInitializer::getPassword);
        registry.add("minio.storage.bucket-name", () -> TEST_BUCKET_NAME);
    }

    @BeforeAll
    static void initBucket() {
        MinioInitializer.createBucket(TEST_BUCKET_NAME);
    }
}
