package com.waynehays.cloudfilestorage.storage;

import com.waynehays.cloudfilestorage.storage.dto.StorageItem;

import java.io.InputStream;
import java.util.Optional;

public interface ResourceStorageApi {

    Optional<StorageItem> getObject(String objectKey);

    void putObject(InputStream inputStream, String objectKey, long size, String contentType);

    void createDirectory(String objectKey);

    void moveObject(String sourceKey, String targetKey);

    void deleteObject(String objectKey);

    void deleteByPrefix(String prefix);
}
