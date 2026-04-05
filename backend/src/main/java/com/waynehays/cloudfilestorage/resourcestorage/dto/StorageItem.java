package com.waynehays.cloudfilestorage.resourcestorage.dto;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
