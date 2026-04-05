package com.waynehays.cloudfilestorage.maintenance.storagequota;

import com.waynehays.cloudfilestorage.dto.response.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaReconciler implements StorageQuotaReconcilerApi {
    private final StorageQuotaPageReconciler pageReconciler;

    @Override
    public void reconcileStorageQuotas() {
        log.info("User quota reconciliation started");

        int currentPage = 0;
        int totalUsersProcessed = 0;
        Page<UserDto> users;

        do {
            try {
                users = pageReconciler.reconcilePage(currentPage);
                totalUsersProcessed += users.getNumberOfElements();
                currentPage++;
            } catch (Exception e) {
                log.error("Quota reconciliation failed on page {}", currentPage, e);
                return;
            }
        } while (users.hasNext());

        log.info("Quota reconciliation completed: {} users processed", totalUsersProcessed);
    }
}
