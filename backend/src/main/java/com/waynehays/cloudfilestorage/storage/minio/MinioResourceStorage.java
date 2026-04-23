package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.storage.minio.config.MinioStorageProperties;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageTransientException;
import com.waynehays.cloudfilestorage.storage.api.ResourceStorageApi;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
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
public class MinioResourceStorage implements ResourceStorageApi {
    private static final String RETRY_NAME = "minioStorage";
    private static final String CIRCUIT_BREAKER_NAME = "minioStorage";

    private static final String MSG_FAILED_GET = "Failed to get object with key: %s";
    private static final String MSG_FAILED_DELETE = "Failed to delete object with key: %s";
    private static final String MSG_FAILED_PUT = "Failed to put object with key: %s";
    private static final String MSG_FAILED_COPY = "Failed to copy object from %s to %s";

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
    public Optional<StorageItem> getObject(String objectKey) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(objectKey)
                            .build()
            );
            return Optional.of(new StorageItem(inputStream));
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new ResourceStorageOperationException(MSG_FAILED_GET.formatted(objectKey), e);
        } catch (IOException e) {
            throw new ResourceStorageTransientException(MSG_FAILED_GET.formatted(objectKey), e);
        } catch (Exception e) {
            throw new ResourceStorageOperationException(MSG_FAILED_GET.formatted(objectKey), e);
        }
    }

    @Override
    public void putObject(InputStream inputStream, String objectKey, long size, String contentType) {
        executeWithExceptionHandling(
                () -> minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(objectKey)
                                .stream(inputStream, size, -1)
                                .contentType(contentType)
                                .build()),
                MSG_FAILED_PUT.formatted(objectKey)
        );
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void moveObject(String sourceKey, String targetKey) {
        copyObject(sourceKey, targetKey);
        try {
            deleteObject(sourceKey);
        } catch (ResourceStorageTransientException e) {
            rollbackCopy(targetKey);
            throw e;
        } catch (Exception e) {
            rollbackCopy(targetKey);
            throw new ResourceStorageOperationException("Failed to move object from %s to %s"
                    .formatted(sourceKey, targetKey), e);
        }
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteObject(String objectKey) {
        executeWithExceptionHandling(
                () -> minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(objectKey)
                                .build()),
                MSG_FAILED_DELETE.formatted(objectKey)
        );
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteByPrefix(String prefix) {
        List<String> keys = new ArrayList<>();
        Iterable<Result<Item>> objects = getListByPrefix(prefix);

        for (Result<Item> result : objects) {
            try {
                keys.add(result.get().objectName());
            } catch (IOException e) {
                throw new ResourceStorageTransientException(MSG_FAILED_DELETE.formatted(prefix), e);
            } catch (Exception e) {
                throw new ResourceStorageOperationException(MSG_FAILED_DELETE.formatted(prefix), e);
            }
        }

        deleteList(keys);
    }

    @Override
    @Retry(name = RETRY_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteList(List<String> keys) {
        List<DeleteObject> objects = keys.stream()
                .map(DeleteObject::new)
                .toList();
        int batchSize = properties.deletionBatchSize();

        for (int i = 0; i < objects.size(); i += batchSize) {
            List<DeleteObject> batch = objects.subList(i, Math.min(i + batchSize, objects.size()));
            flushDeleteBatch(new ArrayList<>(batch));
        }
    }

    private void copyObject(String sourceKey, String targetKey) {
        executeWithExceptionHandling(
                () -> minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(properties.bucketName())
                        .object(targetKey)
                        .source(CopySource.builder()
                                .bucket(properties.bucketName())
                                .object(sourceKey)
                                .build())
                        .build()),
                MSG_FAILED_COPY.formatted(sourceKey, targetKey)
        );
    }

    private void rollbackCopy(String targetKey) {
        try {
            deleteObject(targetKey);
        } catch (Exception e) {
            log.warn("Failed to rollback copy: {}", targetKey, e);
        }
    }

    private Iterable<Result<Item>> getListByPrefix(String prefix) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.bucketName())
                        .prefix(prefix)
                        .recursive(true)
                        .build()
        );
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
            throw new ResourceStorageOperationException("Failed to delete some objects: " + result.failedKeys());
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
            throw new ResourceStorageOperationException(errorMessage, e);
        }
    }
}
