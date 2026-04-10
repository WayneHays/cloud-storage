package com.waynehays.cloudfilestorage.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class MinioTestContainerInitializer {
    private static final String IMAGE = "minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1";
    private static final String USER = "minioadmin";
    private static final String PASSWORD = "minioadmin123";
    private static final String URL_TEMPLATE = "http://%s:%d";
    private static final int PORT = 9000;

    private static final GenericContainer<?> CONTAINER;

    static {
        CONTAINER = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", USER)
                .withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
                .withCommand("server /data");
        CONTAINER.start();
    }

    public static String getUrl() {
        return URL_TEMPLATE.formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(PORT));
    }

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }
}
