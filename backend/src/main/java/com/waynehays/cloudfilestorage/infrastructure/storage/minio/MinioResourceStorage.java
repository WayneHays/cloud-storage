package com.waynehays.cloudfilestorage.infrastructure.storage.minio;

import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageTransientException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class MinioResourceStorage implements ResourceStorageApi {
    private static final String RETRY_NAME = "minioStorage";
    private static final String CIRCUIT_BREAKER_NAME = "minioStorage";

    private static final String MSG_FAILED_GET = "Failed to get object with key: %s";
    private static final String MSG_FAILED_DELETE = "Failed to delete object with key: %s";
    private static final String MSG_FAILED_PUT = "Failed to put object with key: %s";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @FunctionalInterface
    private interface VoidStorageOperation {
        void execute() throws Exception;
    }

    private record BatchDeleteResult(List<String> failedKeys, boolean hasProcessingError) {
        boolean hasFailures() {
            return !failedKeys.isEmpty() || hasProcessingError;
        }
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Optional<StorageItem> getObject(String storageKey) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storageKey)
                            .build()
            );
            return Optional.of(new StorageItem(inputStream));
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new ResourceStorageException(MSG_FAILED_GET.formatted(storageKey), e);
        } catch (IOException e) {
            throw new ResourceStorageTransientException(MSG_FAILED_GET.formatted(storageKey), e);
        } catch (Exception e) {
            throw new ResourceStorageException(MSG_FAILED_GET.formatted(storageKey), e);
        }
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void putObject(InputStream inputStream, String storageKey, long size, String contentType) {
        executeWithExceptionHandling(
                () -> minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(storageKey)
                                .stream(inputStream, size, -1)
                                .contentType(contentType)
                                .build()),
                MSG_FAILED_PUT.formatted(storageKey)
        );
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteObject(String storageKey) {
        executeWithExceptionHandling(
                () -> minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(storageKey)
                                .build()),
                MSG_FAILED_DELETE.formatted(storageKey)
        );
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteList(List<String> storageKeys) {
        List<DeleteObject> objects = storageKeys.stream()
                .map(DeleteObject::new)
                .toList();
        int batchSize = properties.deletionBatchSize();

        for (int i = 0; i < objects.size(); i += batchSize) {
            List<DeleteObject> batch = objects.subList(i, Math.min(i + batchSize, objects.size()));
            flushDeleteBatch(new ArrayList<>(batch));
        }
    }

    private void flushDeleteBatch(List<DeleteObject> batch) {
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(properties.bucketName())
                        .objects(batch)
                        .build()
        );

        BatchDeleteResult result = collectFailures(results);

        if (result.hasFailures()) {
            log.error("Failed to delete objects: {}", result.failedKeys());
            throw new ResourceStorageException("Failed to delete some objects: " + result.failedKeys());
        }
    }

    private BatchDeleteResult collectFailures(Iterable<Result<DeleteError>> results) {
        List<String> failedKeys = new ArrayList<>();
        boolean hasProcessingError = false;

        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                failedKeys.add(error.objectName());
            } catch (IOException e) {
                throw new ResourceStorageTransientException("Failed to process batch delete results", e);
            } catch (Exception e) {
                log.error("Error while processing delete object result", e);
                hasProcessingError = true;
            }
        }

        return new BatchDeleteResult(failedKeys, hasProcessingError);
    }

    private void executeWithExceptionHandling(VoidStorageOperation operation, String errorMessage) {
        try {
            operation.execute();
        } catch (IOException e) {
            throw new ResourceStorageTransientException(errorMessage, e);
        } catch (Exception e) {
            throw new ResourceStorageException(errorMessage, e);
        }
    }
}
