package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;

import java.util.List;
import java.util.Set;

interface ResourceMetadataRepositoryCustom {

    Set<String> findMissingPaths(Long userId, Set<String> paths);

    void batchSaveDirectories(Long userId, List<DirectoryRowDto> directories);

    void batchSaveFiles(Long userId, List<FileRowDto> files);

    DeleteDirectoryResult markFilesForDeletionAndCollectKeys(Long userId, String normalizedPath);
}
