package com.waynehays.cloudfilestorage.scheduler.reconciliation;

import com.waynehays.cloudfilestorage.quota.config.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.quota.service.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaReconciliationService implements StorageQuotaReconciliationServiceApi {
    private final StorageQuotaProperties properties;
    private final StorageQuotaServiceApi quotaService;

    @Override
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");

        int currentPage = 0;
        int totalUsersProcessed = 0;

        while (true) {
            try {
                Page<Long> userIds = quotaService.findAllUserIds(currentPage, properties.reconciliationBatchSize());

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
