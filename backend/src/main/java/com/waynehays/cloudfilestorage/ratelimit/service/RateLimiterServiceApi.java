package com.waynehays.cloudfilestorage.ratelimit.service;

import com.waynehays.cloudfilestorage.ratelimit.dto.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.ratelimit.dto.RequestData;

public interface RateLimiterServiceApi {

    RateLimitCheckResult checkRateLimit(RequestData requestData);
}
