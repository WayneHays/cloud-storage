package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMetadataMapper {

    ResourceMetadataDto toResourceMetadataDto(ResourceMetadata metadata);

    List<ResourceMetadataDto> toResourceMetadataDto(List<ResourceMetadata> entities);

    default ResourceMetadata toDirectoryEntity(Long userId, String path) {
        String dirPath = PathUtils.ensureTrailingSlash(path);
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(userId);
        metadata.setPath(dirPath);
        metadata.setParentPath(PathUtils.extractParentPath(dirPath));
        metadata.setName(PathUtils.extractFilename(dirPath));
        metadata.setType(ResourceType.DIRECTORY);
        metadata.setMarkedForDeletion(false);
        return metadata;
    }
}
