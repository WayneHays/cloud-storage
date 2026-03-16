package com.waynehays.cloudfilestorage.component.keyresolver;

import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.springframework.stereotype.Component;

@Component
public class StorageKeyResolver implements StorageKeyResolverApi {
    private static final String USER_DIRECTORY_FORMAT = "user-%d-files/%s";
    private static final String ROOT_DIRECTORY = "";

    @Override
    public String extractPath(Long userId, String objectKey) {
        String prefixToRoot = resolveKeyToRoot(userId);
        return objectKey.substring(prefixToRoot.length());
    }

    @Override
    public String resolveKeyToRoot(Long userId) {
        return resolveKey(userId, ROOT_DIRECTORY);
    }

    @Override
    public String resolveKeyToDirectory(Long userId, String path) {
        return resolveKey(userId, PathUtils.ensureTrailingSeparator(path));
    }

    @Override
    public String resolveKey(Long userId, String path) {
       return USER_DIRECTORY_FORMAT.formatted(userId, path);
    }
}
