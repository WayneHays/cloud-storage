package com.waynehays.cloudfilestorage.dto.internal;

import com.waynehays.cloudfilestorage.service.storage.InputStreamSupplier;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
