package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.info.ResourceInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceInfoServiceTest {

    @Mock
    private ResourceDtoMapper mapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceInfoService service;

    private static final Long USER_ID = 1L;

    @Test
    void shouldReturnMappedResource() {
        // given
        ResourceMetadataDto metadata = new ResourceMetadataDto(
                1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                100L, ResourceType.FILE);
        ResourceDto expected = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

        when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(metadata);
        when(mapper.fromResourceMetadataDto(metadata)).thenReturn(expected);

        // when
        ResourceDto result = service.getInfo(USER_ID, "docs/file.txt");

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenResourceNotFound() {
        // given
        when(metadataService.findOrThrow(USER_ID, "missing.txt"))
                .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

        // when & then
        assertThatThrownBy(() -> service.getInfo(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
