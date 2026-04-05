package com.waynehays.cloudfilestorage.dto.internal;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
