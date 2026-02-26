package com.waynehays.cloudfilestorage.service.fileinfo;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.entity.FileInfo;

import java.util.List;

public interface FileInfoService {

    FileInfo save(Long userId, FileData fileData, String storageKey);

    String deleteAndReturnStorageKey(Long userId, String directory, String filename);

    FileInfo find(Long userId, String directory, String filename);

    void delete(Long userId, String directory, String filename);

    FileInfo move(Long userId, String directory, String filename, String newDirectory, String newFilename, String newStorageKey);

    List<FileInfo> findAllInDirectoryRecursive(Long userId, String directory);
}
