package com.waynehays.cloudfilestorage.files.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class CleanupJob implements SchedulingConfigurer {
    private final CleanupProperties properties;
    private final CleanupServiceApi service;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                () -> service.processDeletedFiles(properties.limit()),
                properties.interval());
    }
}
