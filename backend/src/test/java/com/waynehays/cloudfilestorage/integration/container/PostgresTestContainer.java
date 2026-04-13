package com.waynehays.cloudfilestorage.integration.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresTestContainer {
    private static final String POSTGRES_VERSION = "postgres:16";
    private static final String DATABASE_NAME = "test";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_VERSION))
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername(USERNAME)
                    .withPassword(PASSWORD);

    static {
        try {
            CONTAINER.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Postgres test container", e);
        }
    }

    public static String getJdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    public static String getUsername() {
        return CONTAINER.getUsername();
    }

    public static String getPassword() {
        return CONTAINER.getPassword();
    }

    private PostgresTestContainer() {
    }
}
