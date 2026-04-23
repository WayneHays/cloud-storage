package com.waynehays.cloudfilestorage.ratelimit.dto;

public record RequestData(Long userId, String endpoint, String httpMethod) {
}
