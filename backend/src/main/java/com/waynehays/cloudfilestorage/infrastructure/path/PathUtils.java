package com.waynehays.cloudfilestorage.infrastructure.path;

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

    public static String extractParentPath(String path) {
        String cleanPath = isDirectory(path)
                ? path.substring(0, path.length() - 1)
                : path;
        return FilenameUtils.getPath(cleanPath);
    }

    public static String extractName(String path) {
        String cleanPath = removeTrailingSlash(path);
        String name = FilenameUtils.getName(cleanPath);
        return isDirectory(path)
                ? name + SLASH
                : name;
    }

    public static String extractDisplayName(String path) {
        String cleanPath = removeTrailingSlash(path);
        return FilenameUtils.getName(cleanPath);
    }

    private static String toDirectoryPath(String filePath) {
        return filePath + SLASH;
    }

    private static String toFilePath(String directoryPath) {
        return directoryPath.substring(0, directoryPath.length() - 1);
    }

    public static String toOppositeTypePath(String path) {
        return isFile(path) ? toDirectoryPath(path) : toFilePath(path);
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
}
