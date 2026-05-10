package com.waynehays.cloudfilestorage.core.quota.listener;

import com.waynehays.cloudfilestorage.core.quota.config.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.core.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaInitializerTest {

    @Mock
    private StorageQuotaProperties properties;

    @Mock
    private StorageQuotaServiceApi service;

    @InjectMocks
    private StorageQuotaInitializer initializer;

    @Test
    @DisplayName("Should create storage quota for new user with limit from properties")
    void shouldCreateStorageQuotaOnUserRegistered() {
        // given
        Long userId = 42L;
        DataSize defaultLimit = DataSize.ofMegabytes(500);
        when(properties.defaultLimit()).thenReturn(defaultLimit);

        // when
        initializer.createStorageQuota(new UserRegisteredEvent(userId));

        // then
        verify(service).createStorageQuota(userId, defaultLimit.toBytes());
    }
}