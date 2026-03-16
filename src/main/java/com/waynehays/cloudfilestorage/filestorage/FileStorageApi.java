package com.waynehays.cloudfilestorage.filestorage;

import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.dto.StorageItem;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface FileStorageApi {

    Optional<StorageItem> getObject(String objectKey);

    void putObject(InputStream inputStream, String objectKey, long size, String contentType);

    void delete(String objectKey);

    void deleteByPrefix(String prefix);

    void move(String sourceKey, String targetKey);

    Optional<MetaData> getMetaData(String objectKey);

    List<MetaData> getList(String prefix);

    List<MetaData> getListRecursive(String prefix);

    void createDirectory(String objectKey);

    boolean exists(String objectKey);
}
