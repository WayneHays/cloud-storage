package com.waynehays.cloudfilestorage.integration.config;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestContainerInitializer {
    private static final String IMAGE = "postgres:16";
    private static final String DB_NAME = "dev-db";
    private static final String DB_USER = "dev-user";
    private static final String DB_PASSWORD = "dev-password";

    private static final PostgreSQLContainer<?> CONTAINER;

    static {
        CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASSWORD);
        CONTAINER.start();
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
}
