package com.waynehays.cloudfilestorage.files.operation.upload.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
class AsyncConfig {
    private final ExecutorProperties properties;

    @Bean
    ExecutorService uploadExecutor() {
        return Executors.newFixedThreadPool(properties.uploadThreadPoolSize());
    }
}
