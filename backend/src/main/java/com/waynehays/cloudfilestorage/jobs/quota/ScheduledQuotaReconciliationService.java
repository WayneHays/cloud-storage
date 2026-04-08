package com.waynehays.cloudfilestorage.jobs.quota;

import com.waynehays.cloudfilestorage.dto.internal.StorageQuotaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledQuotaReconciliationService implements ScheduledQuotaReconciliationServiceApi {
    private final QuotaPageReconciliationService pageReconciler;

    @Override
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");

        int currentPage = 0;
        int totalUsersProcessed = 0;
        Page<StorageQuotaDto> quotas;

        do {
            try {
                quotas = pageReconciler.reconcilePage(currentPage);
                totalUsersProcessed += quotas.getNumberOfElements();
                currentPage++;
            } catch (Exception e) {
                log.error("Quota reconciliation failed on page {}", currentPage, e);
                return;
            }
        } while (quotas.hasNext());

        log.info("Quota reconciliation completed: {} quotas processed", totalUsersProcessed);
    }
}
