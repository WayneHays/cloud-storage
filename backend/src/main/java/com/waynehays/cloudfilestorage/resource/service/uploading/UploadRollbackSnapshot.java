package com.waynehays.cloudfilestorage.resource.service.uploading;

import java.util.List;

record UploadRollbackSnapshot(
        Long userId,
        long totalSize,
        boolean quotaReserved,
        List<String> uploadedToStoragePaths,
        List<String> savedToDbPaths
) {
    boolean hasUploadedToStoragePaths() {
        return !uploadedToStoragePaths.isEmpty();
    }

    boolean hasSavedToDbPaths() {
        return !savedToDbPaths.isEmpty();
    }
}