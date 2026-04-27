package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

record RequestData(Long userId, String endpoint, String httpMethod) {
}
