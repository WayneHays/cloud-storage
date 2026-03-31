package com.waynehays.cloudfilestorage.integration.storage.minio;

import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import com.waynehays.cloudfilestorage.integration.base.AbstractIntegrationBaseTest;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RetryTest extends AbstractIntegrationBaseTest {
    private static final int RETRY_ATTEMPTS = 3;
    private static final int ATTEMPTS_WITHOUT_RETRY = 1;

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ResourceStorageApi resourceStorage;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("minioStorage").reset();
    }

    @Nested
    class GetObject {

        @Test
        void getObject_shouldThrowAfterAllRetriesExhausted() throws Exception {
            // given
            doThrow(new IOException("Connection reset"))
                    .when(minioClient).getObject(any(GetObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> resourceStorage.getObject("test-key"))
                    .isInstanceOf(ResourceStorageTransientException.class);
            verify(minioClient, times(RETRY_ATTEMPTS))
                    .getObject(any(GetObjectArgs.class));
        }

        @Test
        void getObject_shouldNotRetryOnPermanentError() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Invalid bucket"))
                    .when(minioClient).getObject(any(GetObjectArgs.class));

            // when & then
            assertThrows(ResourceStorageOperationException.class,
                    () -> resourceStorage.getObject("test-key"));
            verify(minioClient, times(ATTEMPTS_WITHOUT_RETRY))
                    .getObject(any(GetObjectArgs.class));
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void createDirectory_shouldThrowAfterAllRetriesExhausted() throws Exception {
            doThrow(new IOException("Connection reset"))
                    .when(minioClient).putObject(any(PutObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> resourceStorage.createDirectory("user-1-files/dir/"))
                    .isInstanceOf(ResourceStorageTransientException.class);

            verify(minioClient, times(RETRY_ATTEMPTS))
                    .putObject(any(PutObjectArgs.class));
        }

        @Test
        void createDirectory_shouldNotRetryOnPermanentError() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Invalid bucket"))
                    .when(minioClient).putObject(any(PutObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> resourceStorage.createDirectory("user-1-files/dir/"))
                    .isInstanceOf(ResourceStorageOperationException.class);

            verify(minioClient, times(ATTEMPTS_WITHOUT_RETRY))
                    .putObject(any(PutObjectArgs.class));
        }
    }

    @Nested
    class DeleteObject {

        @Test
        void deleteObject_shouldThrowAfterAllRetriesExhausted() throws Exception {
            // given
            doThrow(new IOException("Connection reset"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> resourceStorage.deleteObject("test-key"))
                    .isInstanceOf(ResourceStorageTransientException.class);

            verify(minioClient, times(RETRY_ATTEMPTS)).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void deleteObject_shouldNotRetryOnPermanentError() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Invalid bucket"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // when & then
            assertThrows(ResourceStorageOperationException.class,
                    () -> resourceStorage.deleteObject("test-key"));

            verify(minioClient, times(ATTEMPTS_WITHOUT_RETRY))
                    .removeObject(any(RemoveObjectArgs.class));
        }
    }

    @Nested
    class MoveObject {

        @Test
        void moveObject_shouldRetryOnTransientCopyFailure() throws Exception {
            // given
            doThrow(new IOException("Connection reset"))
                    .doThrow(new IOException("Connection reset"))
                    .doReturn(null)
                    .when(minioClient).copyObject(any(CopyObjectArgs.class));

            // when
            resourceStorage.moveObject("source-key", "target-key");

            // then
            verify(minioClient, times(RETRY_ATTEMPTS))
                    .copyObject(any(CopyObjectArgs.class));
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void moveObject_shouldRetryOnTransientDeleteFailure() throws Exception {
            // given
            doReturn(null).when(minioClient).copyObject(any(CopyObjectArgs.class));
            doThrow(new IOException("Connection reset"))
                    .doNothing()
                    .doNothing()
                    .doNothing()
                    .doNothing()
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // when & then
            assertThatCode(() -> resourceStorage.moveObject("source-key", "target-key"))
                    .doesNotThrowAnyException();
        }

        @Test
        void moveObject_shouldNotRetryOnPermanentError() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Invalid bucket"))
                    .when(minioClient).copyObject(any(CopyObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> resourceStorage.moveObject("source-key", "target-key"))
                    .isInstanceOf(ResourceStorageOperationException.class);

            verify(minioClient, times(ATTEMPTS_WITHOUT_RETRY))
                    .copyObject(any(CopyObjectArgs.class));
            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        }
    }
}
