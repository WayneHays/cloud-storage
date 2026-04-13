package com.waynehays.cloudfilestorage.integration.infrastructure.storage.minio.resilience4j;

import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test-circuit-breaker")
@DisplayName("MinioResourceStorage circuit breaker tests")
class MinioResourceStorageCircuitBreakerTest extends AbstractResilence4jMinioTest {
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void resetRegistries() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("Should wrap transient IOException into ResourceStorageTransientException")
    void shouldWrapTransientException() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("error"));

        // when & then
        assertThatThrownBy(() -> storage.getObject("key"))
                .isInstanceOf(ResourceStorageTransientException.class);
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("Should open circuit breaker after reaching failure threshold")
    void shouldOpenCircuitBreakerAfterFailures() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("error"));
        int minCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();

        // when
        for (int i = 0; i < minCalls; i++) {
            assertThatThrownBy(() -> storage.getObject("key"))
                    .isInstanceOf(ResourceStorageTransientException.class);
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should reject calls with CallNotPermittedException when circuit breaker is open")
    void shouldRejectCallsWhenOpen() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("service down"));
        int minCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
        for (int i = 0; i < minCalls; i++) {
            try {
                storage.getObject("key");
            } catch (Exception ignored) {
            }
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // when & then
        assertThatThrownBy(() -> storage.getObject("key"))
                .isInstanceOf(CallNotPermittedException.class);
        verify(minioClient, times(minCalls)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("Should keep circuit closed when calls succeed")
    void shouldKeepClosedOnSuccess() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(null, null, null, null, null));

        // when
        int minCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
        for (int i = 0; i < minCalls + 2; i++) {
            storage.getObject("key");
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("should not count non-recorded exceptions as failures")
    void shouldIgnoreNonRecordedExceptions() throws Exception {
        // given
        ErrorResponseException accessDenied = Mockito.mock(ErrorResponseException.class);
        ErrorResponse errorResponse = Mockito.mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("AccessDenied");
        when(accessDenied.errorResponse()).thenReturn(errorResponse);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(accessDenied);
        int minCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();

        // when
        for (int i = 0; i < minCalls + 5; i++) {
            try {
                storage.getObject("key");
            } catch (Exception ignored) {
            }
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
