package com.waynehays.cloudfilestorage.utils;

import com.waynehays.cloudfilestorage.constant.Constants;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class PathUtils {

    public static String normalizeSeparators(String path) {
        return FilenameUtils.separatorsToUnix(path);
    }

    public static String removeTrailingSeparator(String path) {
        return StringUtils.stripEnd(path, Constants.PATH_SEPARATOR);
    }

    public static boolean endsWithSeparator(String path) {
        return path.endsWith(Constants.PATH_SEPARATOR);
    }

    public static String[] splitIntoParts(String path) {
        return path.split(Constants.PATH_SEPARATOR);
    }

    public static String extractParentPath(String path) {
        return FilenameUtils.getPath(path);
    }

    public static String extractFilename(String path) {
        return FilenameUtils.getName(path);
    }

    public static String normalize(String path) {
        String normalized = normalizeSeparators(path.trim());
        return removeTrailingSeparator(normalized);
    }

    public static String stripSeparators(String path) {
        return StringUtils.strip(path, Constants.PATH_SEPARATOR);
    }

    public static String combine(String base, String sub) {
        if (base.isEmpty()) {
            return sub;
        }
        if (sub.isEmpty()) {
            return base;
        }
        return base + Constants.PATH_SEPARATOR + sub;
    }
}
