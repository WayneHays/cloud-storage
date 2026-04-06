package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceDtoMapper {
    String SLASH = "/";

    default ResourceDto fromDto(ResourceMetadataDto dto) {
        String name = dto.isFile()
                ? dto.name()
                : dto.name() + SLASH;

        return createDto(
                dto.path(),
                name,
                dto.size(),
                dto.type()
        );
    }

    default ResourceDto fileFromPath(String path, Long size) {
        return createDto(
                path,
                PathUtils.extractFilename(path),
                size,
                ResourceType.FILE
        );
    }

    default ResourceDto directoryFromPath(String path) {
        return createDto(
                path,
                PathUtils.extractFilename(path) + SLASH,
                null,
                ResourceType.DIRECTORY);
    }

    private ResourceDto createDto(String path, String name, Long size, ResourceType type) {
        String parentPath = PathUtils.extractParentPath(path);
        return new ResourceDto(parentPath, name, size, type);
    }
}
