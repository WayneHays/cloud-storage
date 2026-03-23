package com.waynehays.cloudfilestorage.storage;

import com.waynehays.cloudfilestorage.storage.dto.StorageItem;

import java.io.InputStream;
import java.util.Optional;

public interface ResourceStorageApi {

    void putObject(InputStream inputStream, String objectKey, long size, String contentType);

    void createDirectory(String objectKey);

    void move(String sourceKey, String targetKey);

    void delete(String objectKey);

    void deleteByPrefix(String prefix);

    Optional<StorageItem> getObject(String objectKey);
}
