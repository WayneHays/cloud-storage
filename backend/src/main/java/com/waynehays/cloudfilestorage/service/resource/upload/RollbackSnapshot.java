package com.waynehays.cloudfilestorage.service.resource.upload;

import java.util.List;

record RollbackSnapshot(
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