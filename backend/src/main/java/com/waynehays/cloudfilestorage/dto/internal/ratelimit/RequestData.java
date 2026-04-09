package com.waynehays.cloudfilestorage.dto.internal.ratelimit;

public record RequestData(Long userId, String endpoint, String httpMethod) {
}
