package com.waynehays.cloudfilestorage.jobs;

import com.waynehays.cloudfilestorage.config.properties.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.service.scheduler.quota.StorageQuotaReconciliationServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageQuotaReconciliationJob implements SchedulingConfigurer {
    private final StorageQuotaProperties properties;
    private final StorageQuotaReconciliationServiceApi reconciliationService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                reconciliationService::reconcileStorageQuotas,
                properties.reconciliationInterval());
    }
}
