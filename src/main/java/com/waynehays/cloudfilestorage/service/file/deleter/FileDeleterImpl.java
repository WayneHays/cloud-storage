package com.waynehays.cloudfilestorage.service.file.deleter;

import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeleterImpl implements FileDeleter {
    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;

    @Override
    public void delete(Long userId, String directory, String filename) throws FileNotFoundException {
        String storageKey = fileInfoService.deleteFileInfoAndReturnStorageKey(userId, directory, filename);
        fileStorage.delete(storageKey);
    }
}
