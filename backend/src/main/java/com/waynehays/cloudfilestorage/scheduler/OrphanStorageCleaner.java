package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.CleanupProperties;
import com.waynehays.cloudfilestorage.service.maintenance.OrphanStorageCleanerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanStorageCleaner implements SchedulingConfigurer {
    private final CleanupProperties properties;
    private final OrphanStorageCleanerService cleaner;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(cleaner::clean, properties.interval());
    }
}
