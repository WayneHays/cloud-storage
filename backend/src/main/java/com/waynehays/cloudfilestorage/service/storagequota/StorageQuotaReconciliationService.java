package com.waynehays.cloudfilestorage.service.storagequota;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaReconciliationService {
    private final UserRepository userRepository;
    private final ResourceMetadataServiceApi metadataService;

    @Transactional
    public void reconcile() {
        try {
            List<User> users = userRepository.findAll();

            if (users.isEmpty()) {
                return;
            }

            log.info("Quota reconciliation started: {} users", users.size());

            users.forEach(this::reconcileUser);
        } catch (Exception e) {
            log.error("Quota reconciliation failed", e);
        }
    }

    private void reconcileUser(User user) {
        long actualUsedSpace = metadataService.getUsedSpace(user.getId());

        if (user.getUsedSpace() != actualUsedSpace) {
            log.warn("Quota mismatch for user: id={}, stored={}, actual={}",
                    user.getId(), user.getUsedSpace(), actualUsedSpace);
            user.setUsedSpace(actualUsedSpace);
            userRepository.save(user);
        }
    }
}
