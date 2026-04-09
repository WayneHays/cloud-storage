package com.waynehays.cloudfilestorage.dto.internal.quota;

public record StorageQuotaDto(
        Long userId,
        long usedSpace,
        long storageLimit
) {
}
