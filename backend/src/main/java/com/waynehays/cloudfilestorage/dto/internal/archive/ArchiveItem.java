package com.waynehays.cloudfilestorage.dto.internal.archive;

import com.waynehays.cloudfilestorage.dto.internal.storage.InputStreamSupplier;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
