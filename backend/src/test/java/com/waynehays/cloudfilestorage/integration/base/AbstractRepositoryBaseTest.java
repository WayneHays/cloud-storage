package com.waynehays.cloudfilestorage.integration.base;

import com.waynehays.cloudfilestorage.integration.config.PostgresTestContainerInitializer;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class AbstractRepositoryBaseTest {

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerInitializer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerInitializer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerInitializer::getPassword);
    }
}
