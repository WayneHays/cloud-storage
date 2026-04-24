package com.waynehays.cloudfilestorage.resource.service.metadata;

import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.FileRowDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataServiceApi {

    boolean existsByPath(Long userid, String path);

    ResourceMetadataDto findOrThrow(Long userId, String path);

    List<ResourceMetadataDto> findDirectoryContent(Long userId, String directoryPath);

    List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix);

    List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit);

    List<ResourceMetadataDto> findFilesMarkedForDeletion(int limit);

    Set<String> findExistingPaths(Long userId, Set<String> paths);

    Set<String> findMissingPaths(Long userId, Set<String> paths);

    long markDirectoryForDeletionAndSumSize(Long userId, String path);

    void saveFiles(Long userId, List<FileRowDto> files);

    void saveDirectories(Long userId, List<DirectoryRowDto> directories);

    void saveDirectory(Long userId, String path);

    void moveMetadata(Long userId, String pathFrom, String pathTo);

    void markForDeletion(Long userId, String path);

    void deleteFileByPath(Long userId, String path);

    void deleteDirectoryMetadata(Long userId, String pathPrefix);

    void deleteByPaths(Long userId, List<String> paths);

    void deleteByIds(List<Long> ids);
}
