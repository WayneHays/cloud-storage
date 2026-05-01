package com.waynehays.cloudfilestorage.files.operation.upload;

import java.util.List;

record UploadRollbackDto(
        Long userId,
        long totalSize,
        boolean quotaReserved,
        List<String> uploadedStorageKeys,
        List<String> savedToDbPaths
) {
    boolean hasUploadedStorageKeys() {
        return !uploadedStorageKeys.isEmpty();
    }

    boolean hasSavedToDbPaths() {
        return !savedToDbPaths.isEmpty();
    }
}