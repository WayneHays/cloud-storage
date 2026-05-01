package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
interface ResourceMetadataMapper {

    ResourceMetadataDto toResourceMetadataDto(ResourceMetadata entity);

    List<ResourceMetadataDto> toResourceMetadataDto(List<ResourceMetadata> entities);

    default ResourceMetadata toDirectoryEntity(Long userId, String path) {
        String pathToDirectory = PathUtils.ensureTrailingSlash(path);
        String normalizedPath = PathUtils.normalizePath(pathToDirectory);
        String parentPath = PathUtils.extractParentPath(normalizedPath);
        String displayName = PathUtils.extractDisplayName(pathToDirectory);

        ResourceMetadata entity = new ResourceMetadata();
        entity.setUserId(userId);
        entity.setPath(pathToDirectory);
        entity.setNormalizedPath(normalizedPath);
        entity.setParentPath(parentPath);
        entity.setName(displayName);
        entity.setType(ResourceType.DIRECTORY);
        entity.setMarkedForDeletion(false);
        return entity;
    }
}
