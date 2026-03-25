package com.waynehays.cloudfilestorage.component.converter;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceDtoConverter implements ResourceDtoConverterApi {
    private static final String SLASH = "/";

    public ResourceDto fromMetadata(ResourceMetadata resourceMetadata) {
        String name = resourceMetadata.isFile()
                ? resourceMetadata.getName()
                : resourceMetadata.getName() + SLASH;

        return createDto(
                PathUtils.extractParentPath(resourceMetadata.getPath()),
                name,
                resourceMetadata.getSize(),
                resourceMetadata.getType()
        );
    }

    @Override
    public ResourceDto fileFromPath(String path, Long size) {
        return createDto(
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path),
                size,
                ResourceType.FILE
        );
    }

    @Override
    public ResourceDto directoryFromPath(String path) {
        return createDto(
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path) + SLASH,
                null,
                ResourceType.DIRECTORY);
    }

    private ResourceDto createDto(String path, String name, Long size, ResourceType type) {
        return new ResourceDto(path, name, size, type);
    }
}
