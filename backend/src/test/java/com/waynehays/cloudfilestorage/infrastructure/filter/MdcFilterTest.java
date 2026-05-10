package com.waynehays.cloudfilestorage.infrastructure.filter;

import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

    private static final String MDC_KEY = "userId";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final MdcFilter filter = new MdcFilter();

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should put userId into MDC when authenticated")
    void shouldPutUserIdIntoMdc_whenAuthenticated() throws ServletException, IOException {
        // given
        setAuthenticatedPrincipal(42L);
        doAnswer(invocation -> {
            assertThat(MDC.get(MDC_KEY)).isEqualTo("42");
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not put userId into MDC when no authentication")
    void shouldNotPutUserIdIntoMdc_whenNoAuthentication() throws ServletException, IOException {
        // given
        doAnswer(invocation -> {
            assertThat(MDC.get(MDC_KEY)).isNull();
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear MDC after request completes")
    void shouldClearMdcAfterRequest() throws ServletException, IOException {
        // given
        setAuthenticatedPrincipal(7L);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even when chain throws")
    void shouldClearMdcWhenChainThrows() throws ServletException, IOException {
        // given
        setAuthenticatedPrincipal(7L);
        doThrow(new ServletException("boom")).when(filterChain).doFilter(any(), any());

        // when & then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

    private void setAuthenticatedPrincipal(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "user", "pwd");
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}