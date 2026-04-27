package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class QuotaQuotaReconciliationService implements QuotaReconciliationServiceApi {
    private final QuotaReconciliationProperties properties;
    private final StorageQuotaServiceApi quotaService;

    @Override
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");

        int currentPage = 0;
        int totalUsersProcessed = 0;

        while (true) {
            try {
                Page<Long> userIds = quotaService.findAllUserIds(currentPage, properties.batchSize());

                if (userIds.isEmpty()) {
                    break;
                }
                quotaService.reconcileUsedSpace(userIds.getContent());
                totalUsersProcessed += userIds.getNumberOfElements();
                currentPage++;

                if (!userIds.hasNext()) {
                    break;
                }
            } catch (Exception e) {
                log.error("Quota reconciliation failed on page {}", currentPage, e);
                return;
            }
        }

        log.info("Quota reconciliation completed: {} quotas processed", totalUsersProcessed);
    }
}
