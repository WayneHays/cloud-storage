package com.waynehays.cloudfilestorage.files.operation.search;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.search.config.SearchProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceSearchServiceTest {

    @Mock
    private ResourceResponseMapper mapper;

    @Mock
    private SearchProperties properties;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceSearchService service;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("Should return mapped results")
    void shouldReturnMappedResults() {
        // given
        ResourceMetadataDto metadata = new ResourceMetadataDto(
                1L, USER_ID, "storage-key","docs/report.pdf", "docs/", "report.pdf",
                2048L, ResourceType.FILE);
        ResourceResponse expected = new ResourceResponse("docs/", "report.pdf", 2048L, ResourceType.FILE);

        when(properties.limit()).thenReturn(20);
        when(metadataService.findByNameContaining(USER_ID, "report", 20))
                .thenReturn(List.of(metadata));
        when(mapper.fromResourceMetadataDto(metadata)).thenReturn(expected);

        // when
        List<ResourceResponse> result = service.search(USER_ID, "report");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return empty list when nothing found")
    void shouldReturnEmptyListWhenNothingFound() {
        // given
        when(properties.limit()).thenReturn(20);
        when(metadataService.findByNameContaining(USER_ID, "nonexistent", 20))
                .thenReturn(List.of());

        // when
        List<ResourceResponse> result = service.search(USER_ID, "nonexistent");

        // then
        assertThat(result).isEmpty();
    }
}
