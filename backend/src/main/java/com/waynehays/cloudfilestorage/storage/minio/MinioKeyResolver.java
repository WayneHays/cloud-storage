package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.springframework.stereotype.Component;

@Component
public class MinioKeyResolver implements ResourceStorageKeyResolverApi {
    private static final String USER_DIRECTORY_FORMAT = "user-%d-files/%s";
    private static final String ROOT_DIRECTORY = "";

    public String extractPath(Long userId, String objectKey) {
        String prefixToRoot = resolveKeyToRoot(userId);
        return objectKey.substring(prefixToRoot.length());
    }

    public String resolveKey(Long userId, String path) {
        return USER_DIRECTORY_FORMAT.formatted(userId, path);
    }

    public String resolveKeyToRoot(Long userId) {
        return resolveKey(userId, ROOT_DIRECTORY);
    }
}
