package com.waynehays.cloudfilestorage.core.quota.dto;

public record SpaceReleaseDto(
        Long userId,
        long bytesToRelease
) {
}