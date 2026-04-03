package com.waynehays.cloudfilestorage.service.resource.searcher;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceSearcher implements ResourceSearcherApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        return metadataService.findByNameContaining(userId, query)
                .stream()
                .map(mapper::fromResourceMetadataDto)
                .toList();
    }
}
