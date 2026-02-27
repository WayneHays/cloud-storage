package com.waynehays.cloudfilestorage.service.fileinfo;

import com.waynehays.cloudfilestorage.dto.file.FileData;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;

import java.util.List;

public interface FileInfoService {

    FileInfoDto save(Long userId, FileData fileData, String storageKey);

    FileInfoDto find(Long userId, String directory, String filename);

    FileInfoDto move(Long userId, String directory, String filename, String newDirectory, String newFilename, String newStorageKey);

    void delete(Long userId, String directory, String filename);

    String deleteAndReturnStorageKey(Long userId, String directory, String filename);

    List<FileInfoDto> findAllInDirectoryRecursive(Long userId, String directory);

    List<FileInfoDto> searchByName(Long userId, String name);

    List<FileInfoDto> findInDirectory(Long userId, String directory);
}
