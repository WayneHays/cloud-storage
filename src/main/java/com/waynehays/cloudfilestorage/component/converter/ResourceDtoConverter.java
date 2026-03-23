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

    public ResourceDto fromMetadata(ResourceMetadata resourceMetadata) {
        return ResourceDto.builder()
                .path(PathUtils.extractParentPath(resourceMetadata.getPath()))
                .name(resourceMetadata.getName())
                .size(resourceMetadata.getSize())
                .type(resourceMetadata.getType())
                .build();
    }

    @Override
    public ResourceDto fileFromPath(String path, Long size) {
        return buildDto(path, size, ResourceType.FILE);
    }

    @Override
    public ResourceDto directoryFromPath(String path) {
        return buildDto(path, null, ResourceType.DIRECTORY);
    }

    private ResourceDto buildDto (String path, Long size, ResourceType type) {
        return ResourceDto.builder()
                .path(PathUtils.extractParentPath(path))
                .name(PathUtils.extractFilename(path))
                .size(size)
                .type(type)
                .build();
    }
}
