package com.waynehays.cloudfilestorage.service.storage.dto;

import java.io.InputStream;

public record StorageItem(
        InputStream inputStream
) {
}
