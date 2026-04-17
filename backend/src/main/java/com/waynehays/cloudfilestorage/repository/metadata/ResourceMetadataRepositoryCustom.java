package com.waynehays.cloudfilestorage.repository.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRowDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRowDto;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataRepositoryCustom {

    Set<String> findMissingPaths(Long userId, Set<String> paths);

    void batchSaveDirectories(Long userId, List<DirectoryRowDto> directories);

    void batchSaveFiles(Long userId, List<FileRowDto> files);

    long markForDeletionAndSumSize(Long userId, String prefix);
}
