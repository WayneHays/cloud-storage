package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.entity.ResourceType;
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

    default List<ResourceMetadata> toFileEntity(Long userId, List<NewFileDto> newFiles) {
        return newFiles.stream()
                .map(f -> toFileEntity(userId, f))
                .toList();
    }

    default ResourceMetadata toDirectoryEntity(Long userId, String path) {
        String dirPath = PathUtils.ensureTrailingSlash(path);
        return createEntity(userId, dirPath, ResourceType.DIRECTORY);
    }

    private ResourceMetadata toFileEntity(Long userId, NewFileDto dto) {
        ResourceMetadata metadata = createEntity(userId, dto.path(), ResourceType.FILE);
        metadata.setSize(dto.size());
        return metadata;
    }

    private ResourceMetadata createEntity(Long userId, String path, ResourceType type) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(userId);
        metadata.setPath(path);
        metadata.setParentPath(PathUtils.extractParentPath(path));
        metadata.setName(PathUtils.extractFilename(path));
        metadata.setType(type);
        metadata.setMarkedForDeletion(false);
        return metadata;
    }
}
