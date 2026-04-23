package com.waynehays.cloudfilestorage.scheduler.cleanup;

public interface CleanupServiceApi {

    void clean(int limit);
}
