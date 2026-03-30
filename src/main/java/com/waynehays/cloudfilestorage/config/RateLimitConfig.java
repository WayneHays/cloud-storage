package com.waynehays.cloudfilestorage.config;

import com.waynehays.cloudfilestorage.config.properties.RateLimitProperties;
import com.waynehays.cloudfilestorage.service.ratelimit.BucketRegistry;
import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitRule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {
    private final RateLimitProperties properties;

    @Bean
    public List<RateLimitRule> rateLimitRules() {
        return properties.rules();
    }

    @Bean
    public BucketRegistry bucketRegistry() {
        return new BucketRegistry(properties.bucketExpiration());
    }
}
