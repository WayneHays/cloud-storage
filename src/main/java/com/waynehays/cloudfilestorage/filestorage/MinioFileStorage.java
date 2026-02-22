package com.waynehays.cloudfilestorage.filestorage;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorage implements FileStorage {
    private static final String ERROR_GET = "Failed to get object with key: ";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @Override
    public void put(InputStream inputStream, String key, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucketName())
                    .object(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to put object with key: " + key, e);
        }
    }

    @Override
    public Optional<InputStream> get(String key) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.bucketName())
                        .object(key)
                        .build()
        )) {
            return Optional.of(stream);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new FileStorageException(ERROR_GET + key, e);
        } catch (Exception e) {
            throw new FileStorageException(ERROR_GET + key, e);
        }
    }

    @Override
    public void move(String sourceKey, String targetKey) {
        try {
            copyObject(sourceKey, targetKey);
            deleteSourceWithRollback(sourceKey, targetKey);
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to move object from %s to %s".formatted(sourceKey, targetKey), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucketName())
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete object with key: " + key, e);
        }
    }

    private void copyObject(String sourceKey, String targetKey) throws Exception {
        minioClient.copyObject(CopyObjectArgs.builder()
                .bucket(properties.bucketName())
                .object(targetKey)
                .source(CopySource.builder()
                        .bucket(properties.bucketName())
                        .object(sourceKey)
                        .build())
                .build());
    }

    private void deleteSourceWithRollback(String sourceKey, String targetKey) {
        try {
            delete(sourceKey);
        } catch (Exception deleteException) {
            rollbackCopy(targetKey);
            throw new FileStorageException("Failed to delete source after copy: " + sourceKey, deleteException);
        }
    }

    private void rollbackCopy(String targetKey) {
        try {
            delete(targetKey);
        } catch (Exception rollbackException) {
            log.error("Failed to rollback copy during move: {}", rollbackException.getMessage());
        }
    }
}
