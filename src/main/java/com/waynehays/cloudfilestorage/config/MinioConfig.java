package com.waynehays.cloudfilestorage.config;

import com.waynehays.cloudfilestorage.config.properties.MinioCredentialsProperties;
import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import io.minio.MinioClient;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    private final MinioCredentialsProperties credentialsProperties;
    private final MinioStorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(credentialsProperties.url())
                .credentials(credentialsProperties.accessKey(), credentialsProperties.secretKey())
                .httpClient(createHttpClient())
                .build();
    }

    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(storageProperties.connectTimeout())
                .readTimeout(storageProperties.readTimeout())
                .writeTimeout(storageProperties.writeTimeout())
                .callTimeout(storageProperties.callTimeout())
                .build();
    }
}
