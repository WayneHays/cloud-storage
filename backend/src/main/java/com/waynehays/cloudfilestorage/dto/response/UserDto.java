package com.waynehays.cloudfilestorage.dto.response;

public record UserDto(
        Long id,
        String username,
        long usedSpace
) {
}
