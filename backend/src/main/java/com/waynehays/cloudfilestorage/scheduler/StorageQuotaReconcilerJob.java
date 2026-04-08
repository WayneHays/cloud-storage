package com.waynehays.cloudfilestorage.scheduler;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.jobs.quota.ScheduledQuotaReconciliationServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageQuotaReconcilerJob implements SchedulingConfigurer {
    private final UserStorageProperties properties;
    private final ScheduledQuotaReconciliationServiceApi reconciliationService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                reconciliationService::reconcileStorageQuotas,
                properties.reconciliationInterval());
    }
}
