package com.waynehays.cloudfilestorage.service.ratelimit.dto;

public record RequestData(Long userId, String endpoint, String httpMethod) {
}
