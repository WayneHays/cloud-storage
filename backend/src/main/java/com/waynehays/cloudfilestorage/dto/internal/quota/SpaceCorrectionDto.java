package com.waynehays.cloudfilestorage.dto.internal.quota;

public record SpaceCorrectionDto(
        Long userId,
        long actualUsedSpace
) {
}
