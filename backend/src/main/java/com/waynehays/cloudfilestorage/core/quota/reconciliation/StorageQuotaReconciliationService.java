package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import com.waynehays.cloudfilestorage.core.quota.reconciliation.config.StorageQuotaReconciliationProperties;
import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaBatchApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class StorageQuotaReconciliationService implements StorageQuotaReconciliationServiceApi {
    private final StorageQuotaBatchApi quotaBatchService;
    private final StorageQuotaReconciliationProperties properties;

    @Override
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");

        int currentPage = 0;

        try {
            Pageable pageable = PageRequest.of(0, properties.batchSize());
            Page<Long> page;
            do {
                page = quotaBatchService.findAllUserIds(pageable);
                quotaBatchService.reconcileUsedSpace(page.getContent());
                pageable = page.nextPageable();
                currentPage++;
            } while (page.hasNext());
        } catch (Exception e) {
            log.error("Quota reconciliation failed on page {}", currentPage, e);
        }
    }
}
