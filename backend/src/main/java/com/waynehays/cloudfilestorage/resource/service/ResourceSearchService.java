package com.waynehays.cloudfilestorage.resource.service;

import com.waynehays.cloudfilestorage.resource.config.SearchProperties;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
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
                .map(mapper::fromResourceMetadataDto)
                .toList();
    }
}
