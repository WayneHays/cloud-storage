package com.waynehays.cloudfilestorage.dto.internal;

import com.waynehays.cloudfilestorage.dto.InputStreamSupplier;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
