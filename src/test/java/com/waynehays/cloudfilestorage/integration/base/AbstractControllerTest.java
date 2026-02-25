package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.integration.config.MinioTestContainer;
import com.waynehays.cloudfilestorage.integration.config.PostgresTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractControllerTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer::getPassword);
        registry.add("minio.security.url", MinioTestContainer::getUrl);
        registry.add("minio.security.accessKey", MinioTestContainer::getUser);
        registry.add("minio.security.secretKey", MinioTestContainer::getPassword);
        registry.add("minio.storage.bucketName", MinioTestContainer::getBucket);
    }
}
