package com.waynehays.cloudfilestorage.service.fileservice.filedeleter;

import com.waynehays.cloudfilestorage.exception.FileNotFoundException;

public interface FileDeleter {

    void delete(Long userId, String directory, String filename) throws FileNotFoundException;
}
