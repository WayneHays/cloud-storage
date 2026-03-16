package com.waynehays.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PathUtils {
    private static final String SEPARATOR = "/";

    public static String ensureTrailingSeparator(String path) {
        return path.endsWith(SEPARATOR) ? path : path + SEPARATOR;
    }

    public static boolean isDirectory(String path) {
        return path.endsWith(SEPARATOR);
    }

    public static boolean isFile(String path) {
        return !isDirectory(path);
    }

    public static List<String> getAllDirectories(String path) {
        if (StringUtils.isBlank(path)) {
            return List.of();
        }

        String cleanPath = removeTrailingSeparator(path);
        String[] parts = cleanPath.split(SEPARATOR);
        List<String> directories = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (!sb.isEmpty()) {
                sb.append(SEPARATOR);
            }
            sb.append(part);
            directories.add(sb.toString());
        }

        return directories;
    }

    public static String normalizeSeparators(String path) {
        return FilenameUtils.separatorsToUnix(path);
    }

    public static String removeTrailingSeparator(String path) {
        return StringUtils.stripEnd(path, SEPARATOR);
    }

    public static String extractParentPath(String path) {
        String cleanPath = path.endsWith(SEPARATOR)
                ? path.substring(0, path.length() - 1)
                : path;
        return FilenameUtils.getPath(cleanPath);
    }

    public static String extractFilename(String path) {
        String cleanPath = removeTrailingSeparator(path);
        return FilenameUtils.getName(cleanPath);
    }

    public static String combine(String base, String sub) {
        String cleanBase = removeTrailingSeparator(base);
        String cleanSub = removeTrailingSeparator(sub);

        if (cleanBase.isEmpty()) {
            return cleanSub;
        }
        if (cleanSub.isEmpty()) {
            return cleanBase;
        }
        return cleanBase + SEPARATOR + cleanSub;
    }
}
