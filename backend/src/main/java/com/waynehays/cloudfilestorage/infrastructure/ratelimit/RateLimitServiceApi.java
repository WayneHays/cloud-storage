package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

interface RateLimitServiceApi {

    RateLimitCheckResult checkRateLimit(RequestData requestData);
}
