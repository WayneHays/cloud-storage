package com.waynehays.cloudfilestorage.infrastructure.storage.dto;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
