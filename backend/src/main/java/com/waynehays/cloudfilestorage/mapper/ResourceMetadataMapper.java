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
        applyPath(entity, dirPath);
        entity.setType(ResourceType.DIRECTORY);
        entity.setMarkedForDeletion(false);
        return entity;
    }

    private void applyPath(ResourceMetadata entity, String path) {
        entity.setPath(path);
        entity.setNormalizedPath(PathUtils.normalizePath(path));
        entity.setParentPath(PathUtils.extractParentPath(PathUtils.normalizePath(path)));
        entity.setName(PathUtils.extractDisplayName(path));
    }
}
