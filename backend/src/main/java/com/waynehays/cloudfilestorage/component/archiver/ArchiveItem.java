package com.waynehays.cloudfilestorage.component.archiver;

import com.waynehays.cloudfilestorage.dto.InputStreamSupplier;

public record ArchiveItem(
        String name,
        long size,
        InputStreamSupplier inputStreamSupplier
) {
}
