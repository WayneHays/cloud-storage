package com.waynehays.cloudfilestorage.ratelimiter.dto;

public record RequestData(Long userId, String endpoint, String httpMethod) {
}
