package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.CleanupProperties;
import com.waynehays.cloudfilestorage.service.OrphanResourceCleanerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanResourceCleaner implements SchedulingConfigurer {
    private final CleanupProperties properties;
    private final OrphanResourceCleanerService cleanerService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(cleanerService::clean, Duration.ofSeconds(properties.interval()));
    }
}
