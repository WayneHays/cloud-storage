package com.waynehays.cloudfilestorage.core.metadata.dto;

import java.util.List;

public record DeleteDirectoryResult(long totalSize, List<String> storageKeys) {

    public boolean hasKeys() {
        return !storageKeys.isEmpty();
    }
}
