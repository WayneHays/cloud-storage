package com.waynehays.cloudfilestorage.shared.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class ValidationUtils {
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[\\p{L}\\p{N}._\\- ]+$");
    private static final String CURRENT_DIR = ".";
    private static final String PARENT_DIR = "..";

    public static boolean isInvalidInput(String input) {
        if (input.isBlank()) {
            return true;
        }
        if (PARENT_DIR.equals(input) || CURRENT_DIR.equals(input)) {
            return true;
        }
        if (input.startsWith(CURRENT_DIR) || input.endsWith(CURRENT_DIR)) {
            return true;
        }
        return !ALLOWED_CHARACTERS.matcher(input).matches();
    }
}
