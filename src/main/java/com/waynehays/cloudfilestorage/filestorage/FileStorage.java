package com.waynehays.cloudfilestorage.filestorage;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorage {

    void put (InputStream inputStream, String key, long size, String contentType);

    Optional<InputStream> get(String key);

    void move(String sourceKey, String targetKey);

    void delete(String key);
}
