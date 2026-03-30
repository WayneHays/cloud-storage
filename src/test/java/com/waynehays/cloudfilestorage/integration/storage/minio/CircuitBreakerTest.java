package com.waynehays.cloudfilestorage.integration.storage.minio;

import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import com.waynehays.cloudfilestorage.integration.base.AbstractIntegrationBaseTest;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

class CircuitBreakerTest extends AbstractIntegrationBaseTest {

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private ResourceStorageApi resourceStorage;

    @Test
    void shouldOpenCircuitBreaker_afterFailureThreshold() throws Exception {
        // given
        doThrow(new IOException("Connection reset"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        for (int i = 0; i < 4; i++) {
            try {
                resourceStorage.deleteObject("test-key-" + i);
            } catch (ResourceStorageTransientException | CallNotPermittedException ignored) {
            }
        }

        reset(minioClient);

        assertThatThrownBy(() -> resourceStorage.deleteObject("test-key-next"))
                .isInstanceOf(CallNotPermittedException.class);
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
    }
}
