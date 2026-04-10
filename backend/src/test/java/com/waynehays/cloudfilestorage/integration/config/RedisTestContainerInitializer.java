package com.waynehays.cloudfilestorage.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainerInitializer {
    private static final String IMAGE = "redis:7-alpine";
    private static final int PORT = 6379;

    private static final GenericContainer<?> CONTAINER;

    static {
        CONTAINER = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(PORT);
        CONTAINER.start();
    }

    public static String getHost() {
        return CONTAINER.getHost();
    }

    public static int getPort() {
        return CONTAINER.getMappedPort(PORT);
    }
}
