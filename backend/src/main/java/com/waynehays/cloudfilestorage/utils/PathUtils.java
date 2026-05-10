package com.waynehays.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@UtilityClass
public class PathUtils {
    private static final String SLASH = "/";

    public static boolean isDirectory(String path) {
        return path.endsWith(SLASH);
    }

    public static String ensureTrailingSlash(String path) {
        return isDirectory(path)
                ? path
                : toDirectoryPath(path);
    }

    public static boolean isFile(String path) {
        return !isDirectory(path);
    }

    public static Set<String> getAllAncestorDirectories(String path) {
        if (StringUtils.isBlank(path)) {
            return Set.of();
        }

        String cleanPath = removeTrailingSlash(path);
        String[] parts = cleanPath.split(SLASH);
        int limit = isFile(path)
                ? parts.length - 1
                : parts.length;

        Set<String> directories = new LinkedHashSet<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < limit; i++) {
            if (!sb.isEmpty()) {
                sb.append(SLASH);
            }
            sb.append(parts[i]);
            directories.add(sb + SLASH);
        }
        return directories;
    }

    public static String normalizePath(String path) {
        return path.toLowerCase();
    }

    public static String normalizeSeparators(String path) {
        return FilenameUtils.separatorsToUnix(path);
    }

    public static String removeTrailingSlash(String path) {
        return StringUtils.stripEnd(path, SLASH);
    }

    public static String getParentPath(String path) {
        String cleanPath = isDirectory(path)
                ? removeTrailingSlash(path)
                : path;
        return FilenameUtils.getPath(cleanPath);
    }

    public static String getName(String path) {
        String cleanPath = removeTrailingSlash(path);
        return FilenameUtils.getName(cleanPath);
    }

    public static String combine(String base, String sub) {
        String cleanBase = removeTrailingSlash(base);
        String cleanSub = removeTrailingSlash(sub);

        if (cleanBase.isEmpty()) {
            return cleanSub;
        }
        if (cleanSub.isEmpty()) {
            return cleanBase;
        }
        return cleanBase + SLASH + cleanSub;
    }

    public static String toOppositeTypePath(String path) {
        return isFile(path) ? toDirectoryPath(path) : toFilePath(path);
    }

    private static String toDirectoryPath(String filePath) {
        return filePath + SLASH;
    }

    private static String toFilePath(String directoryPath) {
        return removeTrailingSlash(directoryPath);
    }
}
