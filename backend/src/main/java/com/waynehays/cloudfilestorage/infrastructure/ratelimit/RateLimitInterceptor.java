package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import com.waynehays.cloudfilestorage.core.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
class RateLimitInterceptor implements HandlerInterceptor {
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";

    private final RateLimitServiceApi rateLimiter;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return true;
        }

        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        RequestData requestData = new RequestData(userDetails.id(), endpoint, method);
        RateLimitCheckResult result = rateLimiter.checkRateLimit(requestData);

        if (result.allowed()) {
            if (result.isRateLimited()) {
                response.addHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.remainingTokens()));
            }
            return true;
        } else {
            response.addHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            throw new RateLimitException(endpoint, method, result.retryAfterSeconds());
        }
    }
}
