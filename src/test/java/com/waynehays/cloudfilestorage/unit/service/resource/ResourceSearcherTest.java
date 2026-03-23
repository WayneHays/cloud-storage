package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.searcher.ResourceSearcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceSearcherTest {

    @Mock
    private ResourceMetadataServiceApi service;

    @Mock
    private ResourceDtoConverterApi converter;

    @InjectMocks
    private ResourceSearcher resourceSearcher;

    private static final Long USER_ID = 1L;

    @Nested
    class Search {

        @Test
        void shouldReturnMatchingResources() {
            // given
            String query = "report";
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setPath("directory/report.pdf");
            ResourceDto expectedDto = new ResourceDto("directory/", "report.pdf", 100L, ResourceType.FILE);

            when(service.findByNameContaining(USER_ID, query)).thenReturn(List.of(metadata));
            when(converter.fromMetadata(metadata)).thenReturn(expectedDto);

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        void shouldReturnEmptyListWhenNothingMatches() {
            // given
            String query = "nonexistent";

            when(service.findByNameContaining(USER_ID, query)).thenReturn(List.of());

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).isEmpty();
        }
    }
}
