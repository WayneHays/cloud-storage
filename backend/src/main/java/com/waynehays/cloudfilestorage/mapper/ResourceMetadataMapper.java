package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMetadataMapper {

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
