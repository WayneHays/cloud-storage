package com.waynehays.cloudfilestorage.dto.internal;

public record StorageQuotaDto(
        Long userId,
        long usedSpace,
        long storageLimit
) {
}
