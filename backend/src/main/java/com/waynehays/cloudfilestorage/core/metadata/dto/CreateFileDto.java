package com.waynehays.cloudfilestorage.core.metadata.dto;

public record CreateFileDto(
        String storageKey, String path, long size
) {
}
