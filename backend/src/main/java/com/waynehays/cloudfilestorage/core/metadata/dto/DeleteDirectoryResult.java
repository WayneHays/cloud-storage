package com.waynehays.cloudfilestorage.core.metadata.dto;

import java.util.List;

public record DeleteDirectoryResult(long totalSize, List<String> deletedStorageKeys) {

   public boolean hasKeys() {
        return !deletedStorageKeys.isEmpty();
    }
}
