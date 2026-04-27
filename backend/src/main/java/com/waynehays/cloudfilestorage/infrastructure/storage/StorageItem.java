package com.waynehays.cloudfilestorage.infrastructure.storage;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
