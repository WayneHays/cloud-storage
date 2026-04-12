package com.waynehays.cloudfilestorage.repository.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataRepositoryCustom {

    Set<String> findMissingPaths(Long userId, Set<String> paths);

    void saveDirectories(Long userId, List<DirectoryRow> directories);

    void saveFiles(Long userId, List<FileRow> files);

    long markForDeletionAndSumSize(Long userId, String prefix);
}
