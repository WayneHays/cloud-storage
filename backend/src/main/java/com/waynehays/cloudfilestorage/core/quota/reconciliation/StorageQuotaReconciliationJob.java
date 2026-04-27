package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StorageQuotaReconciliationJob implements SchedulingConfigurer {
    private final StorageQuotaReconciliationProperties properties;
    private final StorageQuotaReconciliationServiceApi service;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                service::reconcileStorageQuotas,
                properties.interval());
    }
}
