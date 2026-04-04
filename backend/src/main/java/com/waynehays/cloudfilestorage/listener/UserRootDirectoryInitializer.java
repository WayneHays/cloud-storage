package com.waynehays.cloudfilestorage.listener;

import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRootDirectoryInitializer {
    private static final String ROOT = "";

    private final ResourceMetadataService metadataService;

    @EventListener(UserRegisteredEvent.class)
    public void createUserRootDirectory(UserRegisteredEvent event) {
        metadataService.saveDirectory(event.userId(), ROOT);
        log.info("Root directory created for user with id: {}", event.userId());
    }
}
