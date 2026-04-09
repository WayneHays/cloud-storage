package com.waynehays.cloudfilestorage.dto.internal.quota;

public record SpaceReleaseDto(
        Long userId,
        long bytes
) {
}
