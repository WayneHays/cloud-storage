package com.waynehays.cloudfilestorage.files.operation.cleanup;

import com.waynehays.cloudfilestorage.files.operation.cleanup.config.CleanupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class CleanupJob {
    private final CleanupProperties properties;
    private final CleanupServiceApi service;

    @Scheduled(fixedRateString = "${cleanup.interval}")
    void processDeletedFiles() {
        service.processDeletedFiles(properties.limit());
    }
}
