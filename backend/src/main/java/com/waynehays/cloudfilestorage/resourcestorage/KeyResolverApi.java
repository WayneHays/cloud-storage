package com.waynehays.cloudfilestorage.resourcestorage;

public interface KeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);
}
