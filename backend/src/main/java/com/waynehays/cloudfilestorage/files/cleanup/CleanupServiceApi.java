package com.waynehays.cloudfilestorage.files.cleanup;

interface CleanupServiceApi {

    void processDeletedFiles(int limit);
}
