package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.CleanupProperties;
import com.waynehays.cloudfilestorage.jobs.deletion.ScheduledDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeletionCleanerJob implements SchedulingConfigurer {
    private final CleanupProperties properties;
    private final ScheduledDeletionService deletionService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                () -> deletionService.clean(properties.limit()),
                properties.interval());
    }
}
