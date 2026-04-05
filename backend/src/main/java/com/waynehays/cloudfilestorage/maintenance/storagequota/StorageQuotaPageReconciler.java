package com.waynehays.cloudfilestorage.maintenance.storagequota;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.quota.UsedSpace;
import com.waynehays.cloudfilestorage.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaPageReconciler {
    private final UserService userService;
    private final StorageQuotaServiceApi quotaService;
    private final UserStorageProperties properties;
    private final ResourceMetadataServiceApi metadataService;

    @Transactional
    public Page<UserDto> reconcilePage(int page) {
        Page<UserDto> users = userService.findAll(
                PageRequest.of(page, properties.reconciliationBatchSize()));
        List<Long> userIds = users.stream().map(UserDto::id).toList();
        Map<Long, Long> actualUsage = metadataService.getUsedSpaceOfUsers(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UsedSpace::getUserId,
                        UsedSpace::getTotalSize
                ));

        users.forEach(u -> {
            long actual = actualUsage.getOrDefault(u.id(), 0L);
            if (u.usedSpace() != actual) {
                log.warn("Quota mismatch for user: {}, stored={}, actual={}",
                        u.id(), u.usedSpace(), actual);
                quotaService.correctUsedSpace(u.id(), actual);
            }
        });

        return users;
    }
}
