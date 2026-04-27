package com.waynehays.cloudfilestorage.files.operation.info;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ResourceInfoService implements ResourceInfoServiceApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, path);
        return mapper.fromResourceMetadataDto(dto);
    }
}
