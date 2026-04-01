package com.waynehays.cloudfilestorage.config;

import com.waynehays.cloudfilestorage.config.properties.ExecutorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class AsyncConfig {
    private final ExecutorProperties properties;

    @Bean
    public ExecutorService moveExecutor() {
        return Executors.newFixedThreadPool(properties.moveThreadPoolSize());
    }
}
