package com.waynehays.cloudfilestorage.infrastructure.storage;

public interface KeyResolverApi {

    String extractPath(Long userId, String objectKey);

    String resolveKey(Long userId, String path);
}
