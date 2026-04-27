package com.waynehays.cloudfilestorage.files.operation.upload;

import java.util.List;

record UploadRollbackDto(
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