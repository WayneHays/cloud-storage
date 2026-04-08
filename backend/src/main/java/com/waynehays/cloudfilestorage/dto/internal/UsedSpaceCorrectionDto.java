package com.waynehays.cloudfilestorage.dto.internal;

public record UsedSpaceCorrectionDto(
        Long userId,
        long actualUsedSpace
) {
}
