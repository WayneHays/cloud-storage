package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;

import java.util.List;

public interface ResourceMetadataServiceApi {

    ResourceMetadata findOrThrow(Long userId, String path);

    boolean exists(Long userId, String path);

    List<ResourceMetadata> findDirectChildren(Long userId, String directoryPath);

    List<ResourceMetadata> findDirectoryContent(Long userId, String pathPrefix);

    List<ResourceMetadata> findByNameContaining(Long userId, String query);

    List<ResourceMetadata> findMarkedForDeletion();

    void saveFile(Long userId, String path, long size);

    void saveDirectory(Long userId, String path);

    void markForDeletion(Long userId, String path);

    void markForDeletionByPrefix(Long userId, String pathPrefix);

    void updatePath(Long userId, String pathFrom, String pathTo);

    void updateContentPaths(List<ResourceMetadata> content, String prefixFrom, String prefixTo);

    void delete(Long userId, String path);

    void deleteByPrefix(Long userId, String pathPrefix);

    void deleteById(Long id);
}
