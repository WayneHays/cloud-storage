package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;

import java.util.List;
import java.util.Set;

public interface ResourceMetadataServiceApi {

    ResourceMetadataDto findOrThrow(Long userId, String path);

    List<ResourceMetadataDto> findDirectChildren(Long userId, String directoryPath);

    List<ResourceMetadataDto> findDirectoryContent(Long userId, String pathPrefix);

    List<ResourceMetadataDto> findByNameContaining(Long userId, String query);

    List<ResourceMetadataDto> findMarkedForDeletion();

    Set<String> findExistingPaths(Long userId, Set<String> paths);

    List<UsedSpace> getUsedSpaceOfUsers(List<Long> userIds);

    long sumResourceSizesByPrefix(Long userId, String prefix);

    void validateDirectoryCreation(Long userId, String path);

    void throwIfAnyExists(Long userId, List<String> paths);

    void saveFile(Long userId, String path, long size);

    void saveDirectories(Long userId, Set<String> paths);

    void updatePathsByPrefix(Long userId, String prefixFrom, String prefixTo);

    void markForDeletion(Long userId, String path);

    void markForDeletionByPrefix(Long userId, String pathPrefix);

    void delete(Long userId, String path);

    void deleteByPrefix(Long userId, String pathPrefix);

    void deleteById(Long id);
}
