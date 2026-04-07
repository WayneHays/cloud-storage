package com.waynehays.cloudfilestorage.service.storage;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.service.storage.dto.StorageItem;

import java.util.List;

public interface ResourceStorageServiceApi {

    void putObject(Long userId, UploadObjectDto uploadObject);

    StorageItem getObject(Long userId, String path);

    void deleteObject(Long userId, String path);

    void deleteObjects(Long userId, List<String> paths);

    void deleteDirectory(Long userId, String path);

    void moveObject(Long userId, String pathFrom, String pathTo);
}
