package com.waynehays.cloudfilestorage.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class MinioTestContainer {
    private static final String IMAGE = "minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1";
    private static final String ROOT_USER = "minioadmin";
    private static final String ROOT_PASSWORD = "minioadmin123";
    private static final String BUCKET_NAME = "test-bucket";
    private static final String URL_TEMPLATE = "http://%s:%d";
    private static final String ENV_ROOT_USER = "MINIO_ROOT_USER";
    private static final String ENV_ROOT_PASSWORD = "MINIO_ROOT_PASSWORD";
    private static final String COMMAND = "server /data";
    private static final int PORT = 9000;

    private static final GenericContainer<?> MINIO;

    static {
        MINIO = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(PORT)
                .withEnv(ENV_ROOT_USER, ROOT_USER)
                .withEnv(ENV_ROOT_PASSWORD, ROOT_PASSWORD)
                .withCommand(COMMAND);
        MINIO.start();
    }

    public static String getUrl() {
        return URL_TEMPLATE.formatted(MINIO.getHost(), MINIO.getMappedPort(PORT));
    }

    public static String getUser() {
        return ROOT_USER;
    }

    public static String getPassword() {
        return ROOT_PASSWORD;
    }

    public static String getBucket() {
        return BUCKET_NAME;
    }
}
