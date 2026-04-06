package com.waynehays.cloudfilestorage.service.resource.infoprovider;

import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceInfoProvider implements ResourceInfoProviderApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, path);
        return mapper.fromDto(dto);
    }
}
