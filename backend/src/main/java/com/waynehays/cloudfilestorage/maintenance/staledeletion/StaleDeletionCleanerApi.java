package com.waynehays.cloudfilestorage.maintenance.staledeletion;

import java.time.Duration;

public interface StaleDeletionCleanerApi {

    void clean(Duration threshold);
}
