package com.waynehays.cloudfilestorage.service.resource.infoprovider;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceInfoProvider implements ResourceInfoProviderApi {
    private final ResourceDtoConverter dtoConverter;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, path);

        return PathUtils.isDirectory(path)
                ? dtoConverter.directoryFromPath(path)
                : dtoConverter.fileFromPath(path, dto.size());
    }
}
