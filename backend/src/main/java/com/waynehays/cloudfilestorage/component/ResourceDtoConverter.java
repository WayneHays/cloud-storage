package com.waynehays.cloudfilestorage.component;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceDtoConverter {
    private static final String SLASH = "/";

    public ResourceDto fromMetadata(ResourceMetadata resourceMetadata) {
        String name = resourceMetadata.isFile()
                ? resourceMetadata.getName()
                : resourceMetadata.getName() + SLASH;

        return createDto(
                resourceMetadata.getPath(),
                name,
                resourceMetadata.getSize(),
                resourceMetadata.getType()
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
