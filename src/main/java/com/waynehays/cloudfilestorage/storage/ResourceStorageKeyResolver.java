package com.waynehays.cloudfilestorage.storage;

public interface ResourceStorageKeyResolver {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);

    String resolveKeyToRoot(Long userId);
}
