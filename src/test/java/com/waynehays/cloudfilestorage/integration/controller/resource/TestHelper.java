package com.waynehays.cloudfilestorage.integration.controller.resource;

import com.waynehays.cloudfilestorage.constant.Constants;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestHelper {

    public static String join(String... parts) {
        return String.join(Constants.PATH_SEPARATOR, parts);
    }
}
