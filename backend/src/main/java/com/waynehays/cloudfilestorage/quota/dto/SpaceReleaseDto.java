package com.waynehays.cloudfilestorage.quota.dto;

public record SpaceReleaseDto(
        Long userId,
        long bytesToRelease
) {
}