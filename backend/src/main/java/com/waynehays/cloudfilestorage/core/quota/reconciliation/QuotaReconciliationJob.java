package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuotaReconciliationJob implements SchedulingConfigurer {
    private final QuotaReconciliationProperties properties;
    private final QuotaReconciliationServiceApi service;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                service::reconcileStorageQuotas,
                properties.interval());
    }
}
