package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.NewDirectoryDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataServiceApi {

    ResourceMetadataDto findOrThrow(Long userId, String path);

    List<ResourceMetadataDto> findDirectoryContent(Long userId, String directoryPath);

    List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix);

    List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit);

    List<ResourceMetadataDto> findFilesMarkedForDeletion(int limit);

    Set<String> findExistingPaths(Long userId, Set<String> paths);

    List<UsedSpace> getUsedSpaceByUsers(List<Long> userIds);

    long markForDeletionAndSumFileSize(Long userId, String path);

    void saveFiles(Long userId, List<NewFileDto> files);

    void saveDirectories(Long userId, List<NewDirectoryDto> newDirectories);

    void saveDirectory(Long userId, String path);

    void updatePathsByPrefix(Long userId, String prefixFrom, String prefixTo);

    void markForDeletion(Long userId, String path);

    void deleteByPath(Long userId, String path);

    void deleteByPathPrefix(Long userId, String pathPrefix);

    void deleteByPaths(Long userId, List<String> paths);

    void deleteAllByIds(List<Long> ids);
}
