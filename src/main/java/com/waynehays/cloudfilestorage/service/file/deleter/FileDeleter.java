package com.waynehays.cloudfilestorage.service.file.deleter;

import com.waynehays.cloudfilestorage.exception.FileNotFoundException;

public interface FileDeleter {

    void delete(Long userId, String directory, String filename) throws FileNotFoundException;
}
