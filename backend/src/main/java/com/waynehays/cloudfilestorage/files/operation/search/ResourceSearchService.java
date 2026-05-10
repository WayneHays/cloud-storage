package com.waynehays.cloudfilestorage.files.operation.search;

import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.search.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ResourceSearchService implements ResourceSearchServiceApi {
    private final SearchProperties properties;
    private final ResourceResponseMapper responseMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceResponse> search(Long userId, String query) {
        return metadataService.findByNameContaining(userId, query, properties.limit())
                .stream()
                .map(responseMapper::fromResourceMetadataDto)
                .toList();
    }
}
