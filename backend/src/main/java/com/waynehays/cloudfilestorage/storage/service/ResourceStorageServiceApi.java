package com.waynehays.cloudfilestorage.storage.service;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.storage.dto.UserPath;

import java.util.List;

public interface ResourceStorageServiceApi {

    void putObject(Long userId, UploadObjectDto uploadObject);

    StorageItem getObject(Long userId, String path);

    void deleteObject(Long userId, String path);

    void deleteObjects(List<UserPath> paths);

    void deleteDirectory(Long userId, String path);

    void moveObject(Long userId, String pathFrom, String pathTo);
}
