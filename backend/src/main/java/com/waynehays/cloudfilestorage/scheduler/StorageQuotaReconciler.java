package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.service.maintenance.StorageQuotaConsistencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageQuotaReconciler implements SchedulingConfigurer {
    private final UserStorageProperties properties;
    private final StorageQuotaConsistencyService consistencyService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(consistencyService::reconcileStorageQuotas, properties.reconciliationInterval());
    }
}
