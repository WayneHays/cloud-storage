package com.waynehays.cloudfilestorage.service.filestorage;

import java.io.InputStream;

public interface FileStorage {

    void put (InputStream inputStream, String key);

    InputStream get(String key);

    void delete(String key);
}
