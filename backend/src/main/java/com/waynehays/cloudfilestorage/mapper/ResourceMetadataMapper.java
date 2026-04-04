package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMetadataMapper {

    ResourceMetadataDto toResourceMetadataDto(ResourceMetadata metadata);

    List<ResourceMetadataDto> toResourceMetadataDto(List<ResourceMetadata> entities);

    default ResourceMetadata toFileEntity(Long userId, NewFileDto dto) {
        return toFileEntity(userId, dto.path(), dto.size());
    }

    default ResourceMetadata toFileEntity(Long userId, String path, Long size) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(userId);
        metadata.setPath(path);
        metadata.setParentPath(PathUtils.extractParentPath(path));
        metadata.setName(PathUtils.extractFilename(path));
        metadata.setSize(size);
        metadata.setType(ResourceType.FILE);
        metadata.setMarkedForDeletion(false);
        return metadata;
    }

    default ResourceMetadata toDirectoryEntity(Long userId, String path) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(userId);
        metadata.setPath(PathUtils.ensureTrailingSlash(path));
        metadata.setParentPath(PathUtils.extractParentPath(path));
        metadata.setName(PathUtils.extractFilename(path));
        metadata.setSize(null);
        metadata.setType(ResourceType.DIRECTORY);
        metadata.setMarkedForDeletion(false);
        return metadata;
    }
}
