package com.waynehays.cloudfilestorage.listener;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserQuotaCreator {
    private final StorageQuotaServiceApi quotaService;
    private final UserStorageProperties properties;

    @EventListener(UserRegisteredEvent.class)
    public void createStorageQuota(UserRegisteredEvent event) {
        quotaService.createStorageQuota(event.userId(), properties.defaultLimit().toBytes());
    }
}
