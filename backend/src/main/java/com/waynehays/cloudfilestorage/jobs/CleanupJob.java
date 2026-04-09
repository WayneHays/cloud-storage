package com.waynehays.cloudfilestorage.jobs;

import com.waynehays.cloudfilestorage.config.properties.CleanupProperties;
import com.waynehays.cloudfilestorage.service.scheduler.cleanup.CleanupServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupJob implements SchedulingConfigurer {
    private final CleanupProperties properties;
    private final CleanupServiceApi cleanupService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                () -> cleanupService.clean(properties.limit()),
                properties.interval());
    }
}
