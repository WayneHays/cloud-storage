package com.waynehays.cloudfilestorage.interceptor;

import com.waynehays.cloudfilestorage.exception.RateLimitException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RequestData;
import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.service.ratelimiter.ApiRateLimiter;
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
public class RateLimitInterceptor implements HandlerInterceptor {
    private final ApiRateLimiter rateLimiter;

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
            if (result.remainingTokens() >= 0) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(result.remainingTokens()));
            }
            return true;
        } else {
            response.addHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            throw new RateLimitException(result.errorMessage(), endpoint, method, result.retryAfterSeconds());
        }
    }
}
