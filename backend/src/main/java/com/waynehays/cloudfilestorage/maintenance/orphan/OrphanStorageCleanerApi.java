package com.waynehays.cloudfilestorage.maintenance.orphan;

public interface OrphanStorageCleanerApi {

    void clean(int limit);
}
