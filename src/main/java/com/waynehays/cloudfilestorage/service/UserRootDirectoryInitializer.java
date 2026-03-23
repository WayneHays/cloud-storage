package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRootDirectoryInitializer {
    private static final String LOG_DIRECTORY_CREATED = "Root directory created for user with id: {}";

    private final ResourceStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String keyToRoot = keyResolver.resolveKeyToRoot(event.userId());
        fileStorage.createDirectory(keyToRoot);
        log.info(LOG_DIRECTORY_CREATED, event.userId());
    }
}
