package com.waynehays.cloudfilestorage.files.operation.search;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ResourceSearchService implements ResourceSearchServiceApi {
    private final ResourceDtoMapper mapper;
    private final SearchProperties properties;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        return metadataService.findByNameContaining(userId, query, properties.limit())
                .stream()
                .map(mapper::fromResourceMetadataDto)
                .toList();
    }
}
