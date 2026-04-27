package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

interface RateLimiterServiceApi {

    RateLimitCheckResult checkRateLimit(RequestData requestData);
}
