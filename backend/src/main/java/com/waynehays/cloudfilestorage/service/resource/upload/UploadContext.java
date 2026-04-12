package com.waynehays.cloudfilestorage.service.resource.upload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UploadContext {
    private final List<String> uploadedToStoragePaths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> savedToDbPaths = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean quotaReserved;

    void addUploadedToStoragePath(String path) {
        uploadedToStoragePaths.add(path);
    }

    void addSavedToDatabasePath(String path) {
        savedToDbPaths.add(path);
    }

    void markQuotaReserved() {
        this.quotaReserved = true;
    }

    List<String> getUploadedToStoragePaths() {
        return List.copyOf(uploadedToStoragePaths);
    }

    List<String> getSavedToDbPaths() {
        return List.copyOf(savedToDbPaths);
    }

    boolean hasAnyUploadedToStoragePaths() {
        return !uploadedToStoragePaths.isEmpty();
    }

    boolean hasAnySavedToDbPaths() {
        return !savedToDbPaths.isEmpty();
    }

    boolean isQuotaReserved() {
        return quotaReserved;
    }
}
