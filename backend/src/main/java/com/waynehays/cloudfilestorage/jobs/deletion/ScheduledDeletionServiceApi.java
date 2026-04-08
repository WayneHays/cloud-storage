package com.waynehays.cloudfilestorage.jobs.deletion;

public interface ScheduledDeletionServiceApi {

    void clean(int limit);
}
