package com.waynehays.cloudfilestorage.infrastructure.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface ResourceStorageApi {

    Optional<StorageItem> getObject(String storageKey);

    void putObject(InputStream inputStream, String storageKey, long size, String contentType);

    void deleteObject(String storageKey);

    void deleteList(List<String> storageKeys);
}
