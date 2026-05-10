package com.waynehays.cloudfilestorage.files.operation.info;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ResourceInfoService implements ResourceInfoServiceApi {
    private final ResourceResponseMapper responseMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceResponse getInfo(Long userId, String path) {
        ResourceMetadataDto dto = metadataService.findByPath(userId, path);
        return responseMapper.fromResourceMetadataDto(dto);
    }
}
