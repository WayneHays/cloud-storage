package com.waynehays.cloudfilestorage.dto.internal;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
