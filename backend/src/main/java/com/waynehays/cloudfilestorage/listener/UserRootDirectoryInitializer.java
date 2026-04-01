package com.waynehays.cloudfilestorage.listener;

import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRootDirectoryInitializer {
    private final ResourceStorageApi resourceStorage;
    private final ResourceStorageKeyResolverApi keyResolver;

    @EventListener(UserRegisteredEvent.class)
    public void createUserRootDirectory(UserRegisteredEvent event) {
        String keyToRoot = keyResolver.resolveKeyToRoot(event.userId());
        resourceStorage.createDirectory(keyToRoot);
        log.info("Root directory created for user with id: {}", event.userId());
    }
}
