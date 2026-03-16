package com.waynehays.cloudfilestorage.filestorage.dto;

import java.io.InputStream;

public record StorageItem(
        MetaData metaData,
        InputStream inputStream
) {
}
