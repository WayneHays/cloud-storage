package com.waynehays.cloudfilestorage.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class MinioTestContainerInitializer {
    private static final String IMAGE = "minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1";
    private static final String KEY_USER = "MINIO_ROOT_USER";
    private static final String KEY_PASSWORD = "MINIO_ROOT_PASSWORD";
    private static final String VALUE_USER = "minioadmin";
    private static final String VALUE_PASSWORD = "minioadmin123";
    private static final String URL_TEMPLATE = "http://%s:%d";
    private static final String COMMAND = "server /data";
    private static final int PORT = 9000;

    private static final GenericContainer<?> CONTAINER;

    static {
        CONTAINER = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(PORT)
                .withEnv(KEY_USER, VALUE_USER)
                .withEnv(KEY_PASSWORD, VALUE_PASSWORD)
                .withCommand(COMMAND);
        CONTAINER.start();
    }

    public static String getUrl() {
        return URL_TEMPLATE.formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(PORT));
    }

    public static String getUser() {
        return VALUE_USER;
    }

    public static String getPassword() {
        return VALUE_PASSWORD;
    }
}
