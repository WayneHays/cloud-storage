package com.waynehays.cloudfilestorage.infrastructure.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResourceStorageServiceApi {

    void putObject(Long userId, String path, long size, String contentType, InputStreamSupplier streamSupplier);

    Optional<StorageItem> getObject(Long userId, String path);

    void deleteObject(Long userId, String path);

    void deleteObjects(Map<Long, List<String>> pathsByUserId);

    void deleteByPrefix(Long userId, String path);

    void moveObject(Long userId, String pathFrom, String pathTo);
}
