package com.waynehays.cloudfilestorage.infrastructure.storage.minio;

import io.minio.MinioClient;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
class MinioConfig {
    private final MinioStorageProperties storageProperties;
    private final MinioSecurityProperties credentialsProperties;

    @Bean
    OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(storageProperties.connectTimeout())
                .readTimeout(storageProperties.readTimeout())
                .writeTimeout(storageProperties.writeTimeout())
                .callTimeout(storageProperties.callTimeout())
                .build();
    }

    @Bean
    MinioClient minioClient(OkHttpClient okHttpClient) {
        return MinioClient.builder()
                .endpoint(credentialsProperties.url())
                .credentials(credentialsProperties.accessKey(), credentialsProperties.secretKey())
                .httpClient(okHttpClient)
                .build();
    }
}
