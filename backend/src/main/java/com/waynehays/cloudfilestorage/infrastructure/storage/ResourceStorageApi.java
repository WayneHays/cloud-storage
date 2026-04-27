package com.waynehays.cloudfilestorage.infrastructure.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface ResourceStorageApi {

    Optional<StorageItem> getObject(String objectKey);

    void putObject(InputStream inputStream, String objectKey, long size, String contentType);

    void moveObject(String sourceKey, String targetKey);

    void deleteObject(String objectKey);

    void deleteByPrefix(String prefix);

    void deleteList(List<String> keys);
}
