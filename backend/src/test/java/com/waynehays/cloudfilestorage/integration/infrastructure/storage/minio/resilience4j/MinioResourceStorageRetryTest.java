package com.waynehays.cloudfilestorage.integration.infrastructure.storage.minio.resilience4j;

import com.waynehays.cloudfilestorage.dto.internal.StorageItem;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import io.github.resilience4j.retry.RetryRegistry;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles({"test-retry"})
@DisplayName("MinioResourceStorage retry tests")
class MinioResourceStorageRetryTest extends AbstractResilence4jMinioTest {

    @Autowired
    private RetryRegistry retryRegistry;

    private int maxAttempts;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker(CB_NAME).reset();
        maxAttempts = retryRegistry.retry(RETRY_NAME).getRetryConfig().getMaxAttempts();
    }

    @Test
    @DisplayName("should retry on transient IOException up to max attempts")
    void shouldRetryOnTransientException() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("network flaky"));

        // when & then
        assertThatThrownBy(() -> storage.getObject("key"))
                .isInstanceOf(ResourceStorageTransientException.class);
        verify(minioClient, times(maxAttempts)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("should succeed on second attempt when first fails transiently")
    void shouldSucceedOnRetry() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("error"))
                .thenReturn(new GetObjectResponse(null, null, null, null, null));

        // when
        Optional<StorageItem> result = storage.getObject("key");

        // then
        assertThat(result).isPresent();
        verify(minioClient, times(2)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("should not retry on non-transient ResourceStorageOperationException")
    void shouldNotRetryOnOperationException() throws Exception {
        // given
        ErrorResponseException accessDenied = Mockito.mock(ErrorResponseException.class);
        ErrorResponse errorResponse = Mockito.mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("AccessDenied");
        when(accessDenied.errorResponse()).thenReturn(errorResponse);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(accessDenied);

        // when & then
        assertThatThrownBy(() -> storage.getObject("key"))
                .isInstanceOf(ResourceStorageOperationException.class);
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("should not retry and return empty when object is missing")
    void shouldNotRetryOnNoSuchKey() throws Exception {
        // given
        ErrorResponseException noSuchKey = Mockito.mock(ErrorResponseException.class);
        ErrorResponse errorResponse = Mockito.mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(noSuchKey.errorResponse()).thenReturn(errorResponse);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(noSuchKey);

        // when
        var result = storage.getObject("key");

        // then
        assertThat(result).isEmpty();
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("should not retry on successful call")
    void shouldNotRetryOnSuccess() throws Exception {
        // given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(null, null, null, null, null));

        // when
        storage.getObject("key");

        // then
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }
}
