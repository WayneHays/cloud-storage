package com.waynehays.cloudfilestorage.service.storagequota;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaConsistencyService {
    private final UserRepository userRepository;
    private final UserStorageProperties properties;
    private final ResourceMetadataServiceApi metadataService;

    @Transactional
    public void reconcileStorageQuotas() {
        log.info("Quota reconciliation started");
        int currentPage = 0;
        int totalUsersProcessed = 0;

        try {
            Page<User> users;

            do {
                users = userRepository.findAll(
                        PageRequest.of(currentPage, properties.reconciliationBatchSize()));
                List<Long> userIds = users.map(User::getId).toList();
                Map<Long, Long> actualStorageUsage = retrieveActualStorageUsageForUsers(userIds);

                for (User user : users) {
                    long actualUsedSpace = actualStorageUsage.getOrDefault(user.getId(), 0L);

                    if (user.getUsedSpace() != actualUsedSpace) {
                        log.warn("Quota mismatch for user: {}, stored={}, actual={}",
                                user.getId(), user.getUsedSpace(), actualUsedSpace);
                        userRepository.updateUsedSpace(user.getId(), actualUsedSpace);
                    }
                }
                totalUsersProcessed += users.getNumberOfElements();
                currentPage++;
            } while (users.hasNext());
            log.info("Quota reconciliation completed: {} users processed", totalUsersProcessed);
        } catch (Exception e) {
            log.error("Quota reconciliation failed on page {}", currentPage, e);
        }
    }

    private Map<Long, Long> retrieveActualStorageUsageForUsers(List<Long> userIds) {
        return metadataService.getUsedSpaceOfUsers(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UsedSpace::getUserId,
                        UsedSpace::getTotalSize
                ));
    }
}
