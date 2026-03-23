package com.waynehays.cloudfilestorage.storage.dto;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
