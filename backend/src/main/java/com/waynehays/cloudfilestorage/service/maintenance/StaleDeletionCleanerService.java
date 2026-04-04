package com.waynehays.cloudfilestorage.service.maintenance;

import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaleDeletionCleanerService {
    private final ResourceMetadataServiceApi metadataService;

    public void clean(Duration threshold) {
        try {
            log.info("Repository orphans cleanup started");

            Instant cutOff = Instant.now().minus(threshold);
            int cleaned = metadataService.deleteStaleDeletionRecords(cutOff);

            log.info("Repository orphan cleanup completed: {} orphans found", cleaned);
        } catch (Exception e) {
            log.error("Repository orphans cleanup job failed", e);
        }
    }
}
