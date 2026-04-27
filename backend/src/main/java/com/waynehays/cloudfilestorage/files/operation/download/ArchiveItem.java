package com.waynehays.cloudfilestorage.files.operation.download;

import com.waynehays.cloudfilestorage.infrastructure.storage.InputStreamSupplier;

record ArchiveItem(String name, long size, InputStreamSupplier inputStreamSupplier) {
}
