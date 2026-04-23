package com.waynehays.cloudfilestorage.storage.api;

public interface KeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);
}
