package com.waynehays.cloudfilestorage.ratelimiter;

import com.waynehays.cloudfilestorage.ratelimiter.dto.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.ratelimiter.dto.RequestData;

public interface RateLimiterApi {

    RateLimitCheckResult checkRateLimit(RequestData requestData);
}
