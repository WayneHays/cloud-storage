package com.waynehays.cloudfilestorage.service.ratelimiter.dto;

public record RequestData(Long userId, String endpoint, String httpMethod) {
}
