package com.waynehays.cloudfilestorage.service.storage.minio;

import com.waynehays.cloudfilestorage.service.storage.KeyResolverApi;
import org.springframework.stereotype.Component;

@Component
public class MinioKeyResolver implements KeyResolverApi {
    private static final String USER_PREFIX_FORMAT = "user-%d-files/";

    public String extractPath(Long userId, String objectKey) {
        String prefix = USER_PREFIX_FORMAT.formatted(userId);
        return objectKey.substring(prefix.length());
    }

    public String resolveKey(Long userId, String path) {
        return USER_PREFIX_FORMAT.formatted(userId) + path;
    }
}
