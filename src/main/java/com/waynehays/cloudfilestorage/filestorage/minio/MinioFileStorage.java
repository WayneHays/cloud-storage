package com.waynehays.cloudfilestorage.filestorage.minio;

import com.waynehays.cloudfilestorage.config.properties.MinioStorageProperties;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.dto.StorageItem;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
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
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorage implements FileStorageApi {
    private static final String DIRECTORY_CONTENT_TYPE = "application/x-directory";
    private static final String MSG_KEY_NOT_EXISTS = "NoSuchKey";
    private static final String MSG_FAILED_GET = "Failed to get object with key: ";
    private static final String MSG_FAILED_PUT = "Failed to put object with key: ";
    private static final String MSG_FAILED_MOVE = "Failed to move from %s to %s";
    private static final String MSG_FAILED_COPY = "Failed to copy object from %s to %s";
    private static final String MSG_FAILED_DELETE = "Failed to delete object with key: ";
    private static final String MSG_FAILED_DELETE_OBJECTS = "Failed to delete objects: ";
    private static final String MSG_FAILED_GET_LIST = "Failed to get list of files by prefix: ";
    private static final String MSG_FAILED_GET_METADATA = "Failed to get meta data from object: ";
    private static final String MSG_FAILED_CREATE_DIRECTORY = "Failed to create directory with key: ";
    private static final String MSG_FAILED_EXTRACT_ITEM = "Failed to extract item from result";
    private static final String LOG_FAILED_ROLLBACK_COPY = "Failed to rollback copy: {}";
    private static final String LOG_FAILED_DELETE_OBJECT = "Failed to delete object: {}";
    private static final String LOG_FAILED_PROCESS_DELETE_RESULT = "Error while processing delete object result";

    private final MinioClient minioClient;
    private final MinioConverter minioConverter;
    private final MinioStorageProperties properties;

    @Override
    public boolean exists(String objectKey) {
        return getMetaData(objectKey).isPresent();
    }

    @Override
    public Optional<MetaData> getMetaData(String objectKey) {
        try {
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(objectKey)
                            .build()
            );

            return Optional.of(minioConverter.toMetaData(response));

        } catch (ErrorResponseException e) {
            if (MSG_KEY_NOT_EXISTS.equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new FileStorageException(MSG_FAILED_GET_METADATA + objectKey, e);
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_GET_METADATA + objectKey, e);
        }
    }

    @Override
    public Optional<StorageItem> getObject(String objectKey) {
        return getMetaData(objectKey).map(metaData -> {
            try {
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(properties.bucketName())
                                .object(objectKey)
                                .build()
                );
                return new StorageItem(metaData, inputStream);
            } catch (Exception e) {
                throw new FileStorageException(MSG_FAILED_GET + objectKey, e);
            }
        });
    }

    @Override
    public List<MetaData> getList(String prefix) {
        return listObjects(prefix, false);
    }

    @Override
    public List<MetaData> getListRecursive(String prefix) {
        return listObjects(prefix, true);
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
            throw new FileStorageException(MSG_FAILED_PUT + objectKey, e);
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
            throw new FileStorageException(MSG_FAILED_CREATE_DIRECTORY + objectKey, e);
        }
    }

    @Override
    public void move(String sourceKey, String targetKey) {
        copyObject(sourceKey, targetKey);
        try {
            delete(sourceKey);
        } catch (Exception e) {
            rollbackCopy(targetKey);
            throw new FileStorageException(MSG_FAILED_MOVE.formatted(sourceKey, targetKey), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_DELETE + objectKey, e);
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        List<DeleteObject> batch = new ArrayList<>();

        for (Result<Item> result : listObjectsRaw(prefix, true)) {
            try {
                batch.add(new DeleteObject(result.get().objectName()));

                if (batch.size() >= properties.batchSize()) {
                    flushDeleteBatch(batch);
                }
            } catch (Exception e) {
                throw new FileStorageException(MSG_FAILED_DELETE + prefix, e);
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
            throw new FileStorageException(MSG_FAILED_COPY.formatted(sourceKey, targetKey), e);
        }
    }

    private void rollbackCopy(String targetKey) {
        try {
            delete(targetKey);
        } catch (Exception e) {
            log.warn(LOG_FAILED_ROLLBACK_COPY, targetKey, e);
        }
    }

    private Iterable<Result<Item>> listObjectsRaw(String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.bucketName())
                        .prefix(prefix)
                        .recursive(recursive)
                        .build()
        );
    }

    private List<MetaData> listObjects(String prefix, boolean recursive) {
        try {
            return StreamSupport.stream(listObjectsRaw(prefix, recursive).spliterator(), false)
                    .map(this::extractItem)
                    .map(minioConverter::toMetaData)
                    .toList();
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_GET_LIST + prefix, e);
        }
    }

    private Item extractItem(Result<Item> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_EXTRACT_ITEM, e);
        }
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
                log.error(LOG_FAILED_DELETE_OBJECT, error.objectName());
            } catch (Exception e) {
                log.error(LOG_FAILED_PROCESS_DELETE_RESULT, e);
            }
        }

        batch.clear();

        if (!failedKeys.isEmpty()) {
            throw new FileStorageException(MSG_FAILED_DELETE_OBJECTS + failedKeys);
        }
    }
}
