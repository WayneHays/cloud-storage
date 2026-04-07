package com.waynehays.cloudfilestorage.service.resource.search;

import com.waynehays.cloudfilestorage.config.properties.SearchProperties;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceSearchService implements ResourceSearchServiceApi {
    private final ResourceDtoMapper mapper;
    private final SearchProperties properties;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        return metadataService.findByNameContaining(userId, query, properties.limit())
                .stream()
                .map(mapper::fromDto)
                .toList();
    }
}
