package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StorageQuotaReconciliationJob {
    private final StorageQuotaReconciliationServiceApi service;

    @Scheduled(fixedRateString = "${reconciliation.interval}")
    void reconcile() {
        service.reconcileStorageQuotas();
    }
}
