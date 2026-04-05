package com.waynehays.cloudfilestorage.resourcestorage.keyresolver;

public interface KeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);
}
