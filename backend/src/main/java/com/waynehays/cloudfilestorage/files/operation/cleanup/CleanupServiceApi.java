package com.waynehays.cloudfilestorage.files.operation.cleanup;

interface CleanupServiceApi {

    void processDeletedFiles(int limit);
}
