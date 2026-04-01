package com.waynehays.cloudfilestorage.component;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceDtoConverter {
    private static final String SLASH = "/";

    public ResourceDto fromMetadata(ResourceMetadataDto dto) {
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

    public ResourceDto fileFromPath(String path, Long size) {
        return createDto(
                path,
                PathUtils.extractFilename(path),
                size,
                ResourceType.FILE
        );
    }

    public ResourceDto directoryFromPath(String path) {
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
