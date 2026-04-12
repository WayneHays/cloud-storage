package com.waynehays.cloudfilestorage.integration.base;

import lombok.experimental.UtilityClass;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@UtilityClass
public final class TestPostgres {

    @SuppressWarnings("resource")
    public static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        try {
            INSTANCE.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Postgres test container", e);
        }
    }
}
