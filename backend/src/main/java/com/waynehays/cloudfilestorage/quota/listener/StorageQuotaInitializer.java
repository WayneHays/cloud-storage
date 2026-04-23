package com.waynehays.cloudfilestorage.quota.listener;

import com.waynehays.cloudfilestorage.quota.config.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.auth.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.quota.service.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageQuotaInitializer {
    private final StorageQuotaProperties properties;
    private final StorageQuotaServiceApi quotaService;

    @EventListener(UserRegisteredEvent.class)
    public void createStorageQuota(UserRegisteredEvent event) {
        quotaService.createStorageQuota(event.userId(), properties.defaultLimit().toBytes());
    }
}
