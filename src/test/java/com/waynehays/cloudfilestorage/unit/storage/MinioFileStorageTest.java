package com.waynehays.cloudfilestorage.unit.storage;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.MinioFileStorage;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageTest {
    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioStorageProperties properties;

    @InjectMocks
    private MinioFileStorage minioFileStorage;

    @BeforeEach
    void setUp() {
        when(properties.bucketName()).thenReturn(BUCKET_NAME);
    }

    @Nested
    @DisplayName("Put method tests")
    class PutMethodTests {
        private final InputStream inputStream = new ByteArrayInputStream("test".getBytes());
        private final String key = "user-1/docs/file.txt";
        private final long size = 1024L;
        private final String contentType = "text/plain";
        private final ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);

        @Test
        @DisplayName("Should successfully upload file to MinIO")
        void shouldSuccessfullyUploadFile() throws Exception {
            // given
            when(minioClient.putObject(any(PutObjectArgs.class)))
                    .thenReturn(mockResponse);

            // when
            minioFileStorage.put(inputStream, key, size, contentType);

            // then
            verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should use correct bucket name, key, contentType")
        void shouldUseCorrectBucketName() throws Exception {
            // given
            when(minioClient.putObject(any(PutObjectArgs.class)))
                    .thenReturn(mockResponse);
            ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

            // when
            minioFileStorage.put(inputStream, key, size, contentType);

            // then
            verify(minioClient).putObject(captor.capture());
            PutObjectArgs capturedArgs = captor.getValue();
            assertThat(capturedArgs.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(capturedArgs.object()).isEqualTo(key);
            assertThat(capturedArgs.contentType()).isEqualTo(contentType);
        }

        @Test
        @DisplayName("Should throw FileStorageException on MinIO error")
        void shouldThrowFileStorageExceptionOnMinioError() throws Exception {
            // given
            when(minioClient.putObject(any(PutObjectArgs.class)))
                    .thenThrow(new IOException("Minio connection error"));

            // when & then
            assertThatThrownBy(() -> minioFileStorage.put(inputStream, key, size, contentType))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Failed to put object");
        }
    }

    @Nested
    @DisplayName("Get method tests")
    class GetMethodTests {
        private final String key = "user-1/docs/file.txt";
        private final GetObjectResponse mockResponse = mock(GetObjectResponse.class);

        @Test
        @DisplayName("Should successfully get file from Minio")
        void shouldSuccessfullyGetFile() throws Exception {
            // given
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenReturn(mockResponse);

            // when
            Optional<InputStream> stream = minioFileStorage.get(key);

            // then
            assertThat(stream).isPresent();
        }

        @Test
        @DisplayName("Should return empty Optional when file not found")
        void shouldReturnEmptyOptionalWhenFileNotFound() throws Exception {
            // given
            ErrorResponseException exception = createNoSuchKeyException();
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow((exception));
            // when
            Optional<InputStream> stream = minioFileStorage.get(key);

            // then
            assertThat(stream).isEmpty();
        }

        @Test
        @DisplayName("Should throw FileStorageException on other MinIO errors")
        void shouldThrowFileStorageExceptionOnOtherErrors() throws Exception {
            // when
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new IOException());

            // when & then
            assertThatThrownBy(() -> minioFileStorage.get(key))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Failed to get object with key: " + key);
        }

        @Test
        @DisplayName("Should use correct bucket and key for get")
        void shouldUseCorrectBucketAndKeyForGet() throws Exception {
            // when
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenReturn(mockResponse);
            ArgumentCaptor<GetObjectArgs> captor = ArgumentCaptor.forClass(GetObjectArgs.class);

            // when
            Optional<InputStream> stream = minioFileStorage.get(key);

            // then
            assertThat(stream).isNotEmpty();
            verify(minioClient).getObject(captor.capture());
            GetObjectArgs capturedArgs = captor.getValue();
            assertThat(capturedArgs.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(capturedArgs.object()).isEqualTo(key);
        }

        private ErrorResponseException createNoSuchKeyException() {
            ErrorResponse errorResponse = new ErrorResponse(
                    "NoSuchKey",
                    "The specified key does not exist",
                    BUCKET_NAME,
                    key,
                    "resource-id",
                    "request-id",
                    "host-id"
            );

            return new ErrorResponseException(errorResponse, null, "GET");
        }
    }

    @Nested
    @DisplayName("Delete method tests")
    class DeleteMethodTests {
        private final String key = "user-1/docs/file.txt";

        @Test
        @DisplayName("Should successfully delete file from MinIO")
        void shouldSuccessfullyDeleteFile() throws Exception {
            // when
            minioFileStorage.delete(key);

            // then
            verify(minioClient, times(1)).removeObject(any());
        }

        @Test
        @DisplayName("Should use correct bucket and key for delete")
        void shouldUseCorrectBucketAndKeyForDelete() throws Exception {
            // given
            ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);

            // when
            minioFileStorage.delete(key);

            // then
            verify(minioClient).removeObject(captor.capture());
            RemoveObjectArgs capturedArgs = captor.getValue();
            assertThat(capturedArgs.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(capturedArgs.object()).isEqualTo(key);
        }

        @Test
        @DisplayName("Should throw FileStorageException on MinIO error")
        void shouldThrowFileStorageExceptionOnDeleteError() throws Exception {
            // given
            doThrow(new IOException("Minio error"))
                    .when(minioClient)
                    .removeObject(any(RemoveObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> minioFileStorage.delete(key))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Failed to delete object");
        }
    }

    @Nested
    @DisplayName("Move method tests")
    class MoveMethodTests {
        private final ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
        private final String sourceKey = "source";
        private final String targetKey = "target";

        @Test
        @DisplayName("Should successfully move file in MinIO")
        void shouldSuccessfullyMoveFile() throws Exception {
            // given
            when(minioClient.copyObject(any(CopyObjectArgs.class)))
                    .thenReturn(mockResponse);
            doNothing()
                    .when(minioClient)
                    .removeObject(any(RemoveObjectArgs.class));

            // when
            minioFileStorage.move(sourceKey, targetKey);

            // then
            verify(minioClient, times(1)).copyObject(any(CopyObjectArgs.class));
            verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should copy before delete")
        void shouldCopyBeforeDelete() throws Exception {
            // given
            when(minioClient.copyObject(any(CopyObjectArgs.class)))
                    .thenReturn(mockResponse);
            doNothing()
                    .when(minioClient)
                    .removeObject(any(RemoveObjectArgs.class));

            // when
            minioFileStorage.move(sourceKey, targetKey);

            // then
            InOrder inOrder = inOrder(minioClient);
            inOrder.verify(minioClient).copyObject(any(CopyObjectArgs.class));
            inOrder.verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should use correct source and target keys")
        void shouldUseCorrectSourceAndTargetKeys() throws Exception {
            // given
            when(minioClient.copyObject(any(CopyObjectArgs.class)))
                    .thenReturn(mockResponse);

            doNothing()
                    .when(minioClient)
                    .removeObject(any(RemoveObjectArgs.class));

            ArgumentCaptor<CopyObjectArgs> captor = ArgumentCaptor.forClass(CopyObjectArgs.class);

            // when
            minioFileStorage.move(sourceKey, targetKey);

            // then
            verify(minioClient).copyObject(captor.capture());
            CopyObjectArgs capturedArgs = captor.getValue();

            assertThat(capturedArgs.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(capturedArgs.object()).isEqualTo(targetKey);

            CopySource source = capturedArgs.source();
            assertThat(source.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(source.object()).isEqualTo(sourceKey);
        }

        @Test
        @DisplayName("Should throw FileStorageException on copy error")
        void shouldThrowFileStorageExceptionOnCopyError() throws Exception {
            // given
            when(minioClient.copyObject(any(CopyObjectArgs.class)))
                    .thenThrow(new IOException());

            // when & then
            assertThatThrownBy(() -> minioFileStorage.move(sourceKey, targetKey))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Failed to move object from %s to %s".formatted(sourceKey, targetKey));
            verify(minioClient).removeObject(argThat(args ->
                    args.object().equals(targetKey)
            ));
        }

        @Test
        @DisplayName("Should throw FileStorageException on delete error after copy")
        void shouldThrowFileStorageExceptionOnDeleteErrorAfterCopy() throws Exception {
            // given
            doThrow(new IOException())
                    .when(minioClient)
                    .removeObject(any(RemoveObjectArgs.class));

            // when & then
            assertThatThrownBy(() -> minioFileStorage.move(sourceKey, targetKey))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Failed to delete source after copy: " + sourceKey);
        }
    }
}
