package com.waynehays.cloudfilestorage.resource.archive;

import com.waynehays.cloudfilestorage.storage.service.InputStreamSupplier;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
