package com.waynehays.cloudfilestorage.service.file.deleter;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeleterImpl implements FileDeleter {
    private static final String MSG_FILE_INFO_NOT_FOUND_IN_DATABASE = "File not found: ";
    private static final String MSG_FAILED_TO_DELETE_FROM_STORAGE = "Failed to delete file from storage";
    private static final String MSG_FAILED_TO_DELETE_FROM_DB = "Failed to delete file info from database";

    private final FileStorage fileStorage;
    private final FileInfoRepository fileInfoRepository;

    @Override
    public void delete(Long userId, String directory, String filename) throws FileNotFoundException {
        FileInfo fileInfo = findFileInfo(userId, directory, filename);
        tryDeleteFromDatabase(fileInfo);
        tryDeleteFromStorage(fileInfo.getStorageKey());
    }

    private FileInfo findFileInfo(Long userId, String directory, String filename) throws FileNotFoundException {
        return fileInfoRepository.findByUserIdAndDirectoryAndName(userId, directory, filename)
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_INFO_NOT_FOUND_IN_DATABASE + directory + Constants.PATH_SEPARATOR + filename));
    }

    private void tryDeleteFromDatabase(FileInfo fileInfo) {
        try {
            fileInfoRepository.delete(fileInfo);
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_TO_DELETE_FROM_DB, e);
        }
    }

    private void tryDeleteFromStorage(String storageKey) {
        try {
            fileStorage.delete(storageKey);
        } catch (Exception e) {
            throw new FileStorageException(MSG_FAILED_TO_DELETE_FROM_STORAGE, e);
        }
    }
}
