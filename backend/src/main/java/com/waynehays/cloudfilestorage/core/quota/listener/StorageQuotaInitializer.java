package com.waynehays.cloudfilestorage.core.quota.listener;

import com.waynehays.cloudfilestorage.core.quota.config.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.core.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class StorageQuotaInitializer {
    private final StorageQuotaProperties properties;
    private final StorageQuotaServiceApi service;

    @EventListener(UserRegisteredEvent.class)
    public void createStorageQuota(UserRegisteredEvent event) {
        service.createStorageQuota(event.userId(), properties.defaultLimit().toBytes());
    }
}
