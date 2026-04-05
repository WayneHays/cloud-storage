package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.OrphanStorageCleanupProperties;
import com.waynehays.cloudfilestorage.maintenance.orphan.OrphanStorageCleanerApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanStorageCleanerJob implements SchedulingConfigurer {
    private final OrphanStorageCleanupProperties properties;
    private final OrphanStorageCleanerApi cleaner;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                () -> cleaner.clean(properties.limit()),
                properties.interval());
    }
}
