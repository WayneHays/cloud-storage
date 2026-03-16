package com.waynehays.cloudfilestorage.service.resource;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRootDirectoryInitializer {
    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String keyToRoot = keyResolver.resolveKeyToRoot(event.userId());
        fileStorage.createDirectory(keyToRoot);
    }
}
