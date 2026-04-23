package com.waynehays.cloudfilestorage.storage.resilience4j;

import com.waynehays.cloudfilestorage.storage.minio.config.MinioSecurityProperties;
import com.waynehays.cloudfilestorage.storage.minio.config.MinioStorageProperties;
import com.waynehays.cloudfilestorage.storage.minio.MinioResourceStorage;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = AbstractResilence4jTest.MinimalConfig.class,
        properties = AbstractResilence4jTest.EXCLUDE_INFRASTRUCTURE_AUTOCONFIG)
public abstract class AbstractResilence4jTest {
    public static final String EXCLUDE_INFRASTRUCTURE_AUTOCONFIG = "spring.autoconfigure.exclude=" +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration," +
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
            "org.springframework.boot.session.autoconfigure.SessionAutoConfiguration";

    protected static final String RETRY_NAME = "minioStorage";
    protected static final String CB_NAME = "minioStorage";

    @Configuration
    @Import({
            MinioResourceStorage.class,
    })
    @EnableConfigurationProperties({MinioStorageProperties.class, MinioSecurityProperties.class})
    @ImportAutoConfiguration({
            AopAutoConfiguration.class,
            CircuitBreakerAutoConfiguration.class,
            RetryAutoConfiguration.class,
    })
    static class MinimalConfig {
    }

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.security.url", () -> "http://stub");
        registry.add("minio.security.access-key", () -> "stub-access");
        registry.add("minio.security.secret-key", () -> "stub-secret");
        registry.add("minio.storage.bucket-name", () -> "stub-bucket");
        registry.add("minio.storage.connect-timeout", () -> "5s");
        registry.add("minio.storage.read-timeout", () -> "5s");
        registry.add("minio.storage.write-timeout", () -> "5s");
        registry.add("minio.storage.call-timeout", () -> "10s");
        registry.add("minio.storage.deletion-batch-size", () -> 5);
    }

    @Autowired
    protected MinioResourceStorage storage;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    protected MinioClient minioClient;
}
