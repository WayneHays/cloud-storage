package com.waynehays.cloudfilestorage.core.metadata.service;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataServiceApi {

    boolean existsByPath(Long userid, String path);

    ResourceMetadataDto findByPath(Long userId, String path);

    List<ResourceMetadataDto> findDirectoryContent(Long userId, String directoryPath);

    List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix);

    List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit);

    List<ResourceMetadataDto> findFilesMarkedForDeletion(int limit);

    DeleteDirectoryResult markDirectoryForDeletionAndCollectKeys(Long userId, String path);

    void throwIfAnyExists(Long userId, List<String> paths);

    void throwIfAnyConflictingTypeExists(Long userId, List<String> paths);

    void saveFiles(Long userId, List<CreateFileDto> files);

    List<ResourceMetadataDto> saveDirectories(Long userId, Set<String> paths);

    ResourceMetadataDto saveDirectory(Long userId, String path);

    void moveMetadata(Long userId, String pathFrom, String pathTo);

    void markForDeletion(Long userId, String path);

    void deleteFileMetadata(Long userId, String path);

    void deleteDirectoryMetadata(Long userId, String pathPrefix);

    void deleteByPaths(Long userId, List<String> paths);

    void deleteByIds(List<Long> ids);
}
