package com.waynehays.cloudfilestorage.integration.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class RedisTestContainer {
    private static final String REDIS_VERSION = "redis:8.6.0";
    private static final int REDIS_PORT = 6379;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> CONTAINER =
            new GenericContainer<>(DockerImageName.parse(REDIS_VERSION))
                    .withExposedPorts(REDIS_PORT);

    static {
        try {
            CONTAINER.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Redis test container", e);
        }
    }

    public static String getHost() {
        return CONTAINER.getHost();
    }

    public static int getPort() {
        return CONTAINER.getMappedPort(REDIS_PORT);
    }

    private RedisTestContainer() {
    }
}
