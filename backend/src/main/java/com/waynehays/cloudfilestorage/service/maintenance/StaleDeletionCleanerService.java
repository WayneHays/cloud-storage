package com.waynehays.cloudfilestorage.service.maintenance;

import com.waynehays.cloudfilestorage.config.properties.CleanupProperties;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaleDeletionCleanerService {
    private final CleanupProperties properties;
    private final ResourceMetadataServiceApi metadataService;

    public void clean() {
        try {
            log.info("Repository orphans cleanup started");

            Instant threshold = Instant.now().minus(properties.interval());
            int cleaned = metadataService.deleteStaleDeletionRecords(threshold);

            log.info("Repository orphan cleanup completed: {} orphans found", cleaned);
        } catch (Exception e) {
            log.error("Repository orphans cleanup job failed", e);
        }
    }
}
