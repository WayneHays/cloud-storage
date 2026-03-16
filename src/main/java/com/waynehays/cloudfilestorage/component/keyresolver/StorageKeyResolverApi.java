package com.waynehays.cloudfilestorage.component.keyresolver;

public interface StorageKeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKeyToRoot(Long userId);

    String resolveKeyToDirectory(Long userId, String path);

    String resolveKey(Long userId, String path);
}
