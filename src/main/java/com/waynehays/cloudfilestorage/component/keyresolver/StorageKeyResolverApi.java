package com.waynehays.cloudfilestorage.component.keyresolver;

public interface StorageKeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);

    String resolveKeyToRoot(Long userId);
}
