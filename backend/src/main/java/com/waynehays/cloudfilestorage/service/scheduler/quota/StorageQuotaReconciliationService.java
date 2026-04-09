package com.waynehays.cloudfilestorage.service.scheduler.quota;

import com.waynehays.cloudfilestorage.config.properties.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpaceCorrectionDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaReconciliationService implements StorageQuotaReconciliationServiceApi {
    private final TransactionTemplate transactionTemplate;
    private final StorageQuotaProperties properties;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");

        int currentPage = 0;
        int totalUsersProcessed = 0;

        while (true) {
            try {
                Page<StorageQuotaDto> quotas = reconcileQuotas(currentPage);
                totalUsersProcessed += quotas.getNumberOfElements();
                currentPage++;

                if (!quotas.hasNext()) {
                    break;
                }
            } catch (Exception e) {
                log.error("Quota reconciliation failed on page {}", currentPage, e);
                return;
            }
        }

        log.info("Quota reconciliation completed: {} quotas processed", totalUsersProcessed);
    }

    private Page<StorageQuotaDto> reconcileQuotas(int page) {
        return transactionTemplate.execute(status -> {
            Page<StorageQuotaDto> quotas = quotaService.findAllQuotas(
                    PageRequest.of(page, properties.batchSize()));
            List<Long> userIds = quotas.stream()
                    .map(StorageQuotaDto::userId)
                    .toList();
            Map<Long, Long> actualUsage = getActualUsageByUsers(userIds);
            List<UsedSpaceCorrectionDto> corrections = new ArrayList<>();

            collectMismatches(quotas, actualUsage, corrections);

            if (!corrections.isEmpty()) {
                quotaService.batchUpdateUsedSpace(corrections);
            }

            return quotas;
        });
    }

    private void collectMismatches(Page<StorageQuotaDto> quotas,
                                   Map<Long, Long> actualUsage,
                                   List<UsedSpaceCorrectionDto> corrections) {
        quotas.forEach(q -> {
            long actual = actualUsage.getOrDefault(q.userId(), 0L);
            if (q.usedSpace() != actual) {
                log.warn("Quota mismatch for user: {}, stored={}, actual={}",
                        q.userId(), q.usedSpace(), actual);
                corrections.add(new UsedSpaceCorrectionDto(q.userId(), actual));
            }
        });
    }

    private Map<Long, Long> getActualUsageByUsers(List<Long> userIds) {
        return metadataService.getUsedSpaceByUsers(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UsedSpace::getUserId,
                        UsedSpace::getTotalSize
                ));
    }
}
