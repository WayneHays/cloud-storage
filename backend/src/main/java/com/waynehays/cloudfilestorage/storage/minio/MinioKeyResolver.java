package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.springframework.stereotype.Component;

@Component
public class MinioKeyResolver implements ResourceStorageKeyResolverApi {
    private static final String USER_PREFIX_FORMAT = "user-%d-files/";

    public String extractPath(Long userId, String objectKey) {
        String prefix = USER_PREFIX_FORMAT.formatted(userId);
        return objectKey.substring(prefix.length());
    }

    public String resolveKey(Long userId, String path) {
        return USER_PREFIX_FORMAT.formatted(userId) + path;
    }
}
