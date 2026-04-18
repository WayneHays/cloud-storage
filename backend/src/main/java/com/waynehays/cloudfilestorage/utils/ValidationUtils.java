package com.waynehays.cloudfilestorage.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class ValidationUtils {
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[\\p{L}\\p{N}._\\- ]+$");
    private static final String CURRENT_DIR = ".";
    private static final String PARENT_DIR = "..";

    public static boolean isInvalidSegment(String segment) {
        if (segment.isBlank()) {
            return true;
        }
        if (PARENT_DIR.equals(segment) || CURRENT_DIR.equals(segment)) {
            return true;
        }
        if (segment.startsWith(CURRENT_DIR) || segment.endsWith(CURRENT_DIR)) {
            return true;
        }
        return !ALLOWED_CHARACTERS.matcher(segment).matches();
    }
}
