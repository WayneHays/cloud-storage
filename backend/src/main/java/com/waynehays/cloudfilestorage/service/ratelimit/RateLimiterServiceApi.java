package com.waynehays.cloudfilestorage.service.ratelimit;

import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RequestData;

public interface RateLimiterServiceApi {

    RateLimitCheckResult checkRateLimit(RequestData requestData);
}
