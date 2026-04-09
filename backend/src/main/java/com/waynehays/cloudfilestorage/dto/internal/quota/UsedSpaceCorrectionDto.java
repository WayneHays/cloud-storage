package com.waynehays.cloudfilestorage.dto.internal.quota;

public record UsedSpaceCorrectionDto(
        Long userId,
        long actualUsedSpace
) {
}
