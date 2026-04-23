package com.waynehays.cloudfilestorage.resource.repository;

import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.FileRowDto;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataRepositoryCustom {

    Set<String> findMissingPaths(Long userId, Set<String> paths);

    void batchSaveDirectories(Long userId, List<DirectoryRowDto> directories);

    void batchSaveFiles(Long userId, List<FileRowDto> files);

    long markForDeletionAndSumSize(Long userId, String prefix);
}
