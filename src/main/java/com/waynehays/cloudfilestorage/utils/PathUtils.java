package com.waynehays.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PathUtils {
    private static final String SLASH = "/";

    public static String ensureTrailingSlash(String path) {
        return path.endsWith(SLASH) ? path : path + SLASH;
    }

    public static boolean isDirectory(String path) {
        return path.endsWith(SLASH);
    }

    public static boolean isFile(String path) {
        return !isDirectory(path);
    }

    public static List<String> getAllDirectories(String path) {
        if (StringUtils.isBlank(path)) {
            return List.of();
        }

        String cleanPath = removeTrailingSlash(path);
        String[] parts = cleanPath.split(SLASH);
        List<String> directories = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (!sb.isEmpty()) {
                sb.append(SLASH);
            }
            sb.append(part);
            directories.add(sb.toString());
        }

        return directories;
    }

    public static String normalizeSeparators(String path) {
        return FilenameUtils.separatorsToUnix(path);
    }

    public static String removeTrailingSlash(String path) {
        return StringUtils.stripEnd(path, SLASH);
    }

    public static String extractParentPath(String path) {
        String cleanPath = path.endsWith(SLASH)
                ? path.substring(0, path.length() - 1)
                : path;
        return FilenameUtils.getPath(cleanPath);
    }

    public static String extractFilename(String path) {
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
}
