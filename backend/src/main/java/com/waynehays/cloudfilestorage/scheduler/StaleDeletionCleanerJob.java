package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.StaleDeletionCleanupProperties;
import com.waynehays.cloudfilestorage.maintenance.staledeletion.StaleDeletionCleanerApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleDeletionCleanerJob implements SchedulingConfigurer {
    private final StaleDeletionCleanupProperties properties;
    private final StaleDeletionCleanerApi cleaner;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                () -> cleaner.clean(properties.threshold()),
                properties.interval());
    }
}
