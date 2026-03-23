package com.waynehays.cloudfilestorage.service.resource.infoprovider;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceInfoProvider implements ResourceInfoProviderApi {
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        ResourceMetadata resourceMetadata = metadataService.findOrThrow(userId, path);

        return PathUtils.isDirectory(path)
                ? dtoConverter.directoryFromPath(path)
                : dtoConverter.fileFromPath(path, resourceMetadata.getSize());
    }
}
