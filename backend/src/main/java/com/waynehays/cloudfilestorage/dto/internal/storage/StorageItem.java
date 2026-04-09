package com.waynehays.cloudfilestorage.dto.internal.storage;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
