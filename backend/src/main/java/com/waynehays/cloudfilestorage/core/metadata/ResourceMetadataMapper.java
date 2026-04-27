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
        String dirPath = PathUtils.ensureTrailingSlash(path);
        ResourceMetadata entity = new ResourceMetadata();
        entity.setUserId(userId);
        entity.setPath(dirPath);
        entity.setNormalizedPath(PathUtils.normalizePath(dirPath));
        entity.setParentPath(PathUtils.extractParentPath(PathUtils.normalizePath(dirPath)));
        entity.setName(PathUtils.extractDisplayName(dirPath));
        entity.setType(ResourceType.DIRECTORY);
        entity.setMarkedForDeletion(false);
        return entity;
    }
}
