package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioResourceStorage implements ResourceStorageApi {
    private static final String DIRECTORY_CONTENT_TYPE = "application/x-directory";
    private static final String MSG_FAILED_GET = "Failed to get object with key: ";
    private static final String MSG_FAILED_DELETE = "Failed to delete object with key: ";
    private static final String MSG_FAILED_PUT = "Failed to put object with key: ";
    private static final String MSG_FAILED_COPY = "Failed to copy object from %s to %s";
    private static final String MSG_FAILED_CREATE_DIRECTORY = "Failed to create directory with key: ";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @FunctionalInterface
    private interface VoidStorageOperation {
        void execute() throws Exception;
    }

    @Override
    @Retry(name = "minioStorage")
    @CircuitBreaker(name = "minioStorage")
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
            throw new ResourceStorageOperationException(MSG_FAILED_GET + objectKey, e);
        } catch (IOException e) {
            throw new ResourceStorageTransientException(MSG_FAILED_GET + objectKey, e);
        } catch (Exception e) {
            throw new ResourceStorageOperationException(MSG_FAILED_GET + objectKey, e);
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
                MSG_FAILED_PUT + objectKey
        );
    }

    @Override
    @Retry(name = "minioStorage")
    @CircuitBreaker(name = "minioStorage")
    public void createDirectory(String objectKey) {
        executeWithExceptionHandling(
                () -> minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .contentType(DIRECTORY_CONTENT_TYPE)
                                .build()),
                MSG_FAILED_CREATE_DIRECTORY + objectKey
        );
    }

    @Override
    @Retry(name = "minioStorage")
    @CircuitBreaker(name = "minioStorage")
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
    @Retry(name = "minioStorage")
    @CircuitBreaker(name = "minioStorage")
    public void deleteObject(String objectKey) {
        executeWithExceptionHandling(
                () -> minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(objectKey)
                                .build()),
                MSG_FAILED_DELETE + objectKey
        );
    }

    @Override
    @Retry(name = "minioStorage")
    @CircuitBreaker(name = "minioStorage")
    public void deleteByPrefix(String prefix) {
        List<DeleteObject> batch = new ArrayList<>();
        Iterable<Result<Item>> objectsToDelete = listObjects(prefix);

        for (Result<Item> result : objectsToDelete) {
            try {
                DeleteObject deleteObject = new DeleteObject(result.get().objectName());
                batch.add(deleteObject);

                if (batch.size() >= properties.batchSize()) {
                    flushDeleteBatch(batch);
                }
            } catch (IOException e) {
                throw new ResourceStorageTransientException(MSG_FAILED_DELETE + prefix, e);
            } catch (Exception e) {
                throw new ResourceStorageOperationException(MSG_FAILED_DELETE + prefix, e);
            }
        }

        if (!batch.isEmpty()) {
            flushDeleteBatch(batch);
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

    private Iterable<Result<Item>> listObjects(String prefix) {
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

        List<String> failedKeys = new ArrayList<>();

        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                failedKeys.add(error.objectName());
                log.error("Failed to delete object: {}", error.objectName());
            } catch (IOException e) {
                throw new ResourceStorageTransientException("Failed to process batch delete results", e);
            } catch (Exception e) {
                log.error("Error while processing delete object result", e);
            }
        }

        batch.clear();

        if (!failedKeys.isEmpty()) {
            throw new ResourceStorageOperationException("Failed to delete objects: " + failedKeys);
        }
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
