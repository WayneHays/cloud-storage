package com.waynehays.cloudfilestorage.jobs.quota;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.dto.internal.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.UsedSpaceCorrectionDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.quota.UsedSpace;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaPageReconciliationService {
    private final UserStorageProperties properties;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    @Transactional
    public Page<StorageQuotaDto> reconcilePage(int page) {
        Page<StorageQuotaDto> quotas = quotaService.findAllQuotas(
                PageRequest.of(page, properties.reconciliationBatchSize()));
        List<Long> userIds = quotas.stream().map(StorageQuotaDto::userId).toList();
        Map<Long, Long> actualUsage = getActualUsageByUsers(userIds);
        List<UsedSpaceCorrectionDto> corrections = new ArrayList<>();

        collectMismatches(quotas, actualUsage, corrections);

        if (!corrections.isEmpty()) {
            quotaService.batchUpdateUsedSpace(corrections);
        }

        return quotas;
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
