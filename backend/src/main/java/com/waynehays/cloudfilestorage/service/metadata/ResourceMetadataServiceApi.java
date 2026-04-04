package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ResourceMetadataServiceApi {

    ResourceMetadataDto findOrThrow(Long userId, String path);

    List<ResourceMetadataDto> findDirectChildren(Long userId, String directoryPath);

    List<ResourceMetadataDto> findFilesByPrefix(Long userId, String prefix);

    List<ResourceMetadataDto> findAllByPrefix(Long userId, String prefix);

    List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit);

    List<ResourceMetadataDto> findMarkedForDeletion(int limit);

    Set<String> findExistingPaths(Long userId, Set<String> paths);

    List<UsedSpace> getUsedSpaceOfUsers(List<Long> userIds);

    long sumResourceSizesByPrefix(Long userId, String prefix);

    void validateDirectoryCreation(Long userId, String path);

    void throwIfAnyExists(Long userId, List<String> paths);

    void saveFiles(Long userId, List<NewFileDto> files);

    void saveDirectories(Long userId, Set<String> paths);

    void saveDirectory(Long userId, String path);

    void updatePathsByPrefix(Long userId, String prefixFrom, String prefixTo);

    void markForDeletion(Long userId, String path);

    void markForDeletionByPrefix(Long userId, String pathPrefix);

    void delete(Long userId, String path);

    void deleteByPrefix(Long userId, String pathPrefix);

    void deleteByPaths(Long userId, List<String> paths);

    void deleteById(Long id);

    int deleteStaleDeletionRecords(Instant threshold);
}
