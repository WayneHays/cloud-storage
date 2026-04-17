package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ResourceDtoMapper {

    List<ResourceDto> fromResourceMetadataDto(List<ResourceMetadataDto> dtos);

    default ResourceDto fromResourceMetadataDto(ResourceMetadataDto dto) {
        String name = dto.isFile()
                ? dto.name()
                : PathUtils.ensureTrailingSlash(dto.name());
        return createDto(dto.path(), name, dto.size(), dto.type());
    }

    default ResourceDto fileFromPath(String path, Long size) {
        return createDto(path, PathUtils.extractFilename(path), size,ResourceType.FILE);
    }

    default ResourceDto directoryFromPath(String path) {
        String name = PathUtils.ensureTrailingSlash(PathUtils.extractFilename(path));
        return createDto(path, name, null, ResourceType.DIRECTORY);
    }

    default List<ResourceDto> directoriesFromPaths(Set<String> paths) {
        return paths.stream()
                .map(this::directoryFromPath)
                .toList();
    }

    private ResourceDto createDto(String path, String name, Long size, ResourceType type) {
        String parentPath = PathUtils.extractParentPath(path);
        return new ResourceDto(parentPath, name, size, type);
    }
}
