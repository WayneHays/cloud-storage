package com.waynehays.cloudfilestorage.infrastructure.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResourceStorageServiceApi {

    void putObject(Long userId, String storageKey, long size, String contentType, InputStreamSupplier streamSupplier);

    Optional<StorageItem> getObject(Long userId, String storageKey);

    void deleteObject(Long userId, String storageKey);

    void deleteObjects(Map<Long, List<String>> pathsByUserId);
}
