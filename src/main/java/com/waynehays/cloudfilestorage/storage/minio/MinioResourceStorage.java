package com.waynehays.cloudfilestorage.storage.minio;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.ResourceStorageException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
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

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @Override
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
            throw new ResourceStorageException(MSG_FAILED_GET + objectKey, e);
        } catch (Exception e) {
            throw new ResourceStorageException(MSG_FAILED_GET + objectKey, e);
        }
    }

    @Override
    public void putObject(InputStream inputStream, String objectKey, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucketName())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new ResourceStorageException("Failed to put object with key: " + objectKey, e);
        }
    }

    @Override
    public void createDirectory(String objectKey) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .contentType(DIRECTORY_CONTENT_TYPE)
                            .build()
            );
        } catch (Exception e) {
            throw new ResourceStorageException("Failed to create directory with key: " + objectKey, e);
        }
    }

    @Override
    public void moveObject(String sourceKey, String targetKey) {
        copyObject(sourceKey, targetKey);
        try {
            deleteObject(sourceKey);
        } catch (Exception e) {
            rollbackCopy(targetKey);
            throw new ResourceStorageException("Failed to move object from %s to %s"
                    .formatted(sourceKey, targetKey), e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw new ResourceStorageException(MSG_FAILED_DELETE + objectKey, e);
        }
    }

    @Override
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
            } catch (Exception e) {
                throw new ResourceStorageException(MSG_FAILED_DELETE + prefix, e);
            }
        }

        if (!batch.isEmpty()) {
            flushDeleteBatch(batch);
        }
    }

    private void copyObject(String sourceKey, String targetKey) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.bucketName())
                    .object(targetKey)
                    .source(CopySource.builder()
                            .bucket(properties.bucketName())
                            .object(sourceKey)
                            .build())
                    .build());
        } catch (Exception e) {
            throw new ResourceStorageException("Failed to copy object from %s to %s"
                    .formatted(sourceKey, targetKey), e);
        }
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
            } catch (Exception e) {
                log.error("Error while processing delete object result", e);
            }
        }

        batch.clear();

        if (!failedKeys.isEmpty()) {
            throw new ResourceStorageException("Failed to delete objects: " + failedKeys);
        }
    }
}
