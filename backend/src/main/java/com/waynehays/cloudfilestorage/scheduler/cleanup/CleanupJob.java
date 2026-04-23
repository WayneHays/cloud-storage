package com.waynehays.cloudfilestorage.scheduler.cleanup;

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
