package com.waynehays.cloudfilestorage.service.resource;

import com.waynehays.cloudfilestorage.config.properties.SearchProperties;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.search.ResourceSearchService;
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
    private ResourceDtoMapper mapper;

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
                1L, USER_ID, "docs/report.pdf", "docs/", "report.pdf",
                2048L, ResourceType.FILE);
        ResourceDto expected = new ResourceDto("docs/", "report.pdf", 2048L, ResourceType.FILE);

        when(properties.limit()).thenReturn(20);
        when(metadataService.findByNameContaining(USER_ID, "report", 20))
                .thenReturn(List.of(metadata));
        when(mapper.fromResourceMetadataDto(metadata)).thenReturn(expected);

        // when
        List<ResourceDto> result = service.search(USER_ID, "report");

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
        List<ResourceDto> result = service.search(USER_ID, "nonexistent");

        // then
        assertThat(result).isEmpty();
    }
}
