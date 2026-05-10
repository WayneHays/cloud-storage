package com.waynehays.cloudfilestorage.files.operation.upload.dto;

import java.util.List;

public record RollbackDto(
        Long userId,
        long totalSize,
        boolean quotaReserved,
        List<String> uploadedStorageKeys,
        List<String> savedToDbPaths
) {
    public boolean hasUploadedStorageKeys() {
        return !uploadedStorageKeys.isEmpty();
    }

    public boolean hasSavedToDbPaths() {
        return !savedToDbPaths.isEmpty();
    }
}