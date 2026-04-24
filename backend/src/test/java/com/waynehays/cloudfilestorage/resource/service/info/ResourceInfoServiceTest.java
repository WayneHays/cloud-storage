package com.waynehays.cloudfilestorage.resource.service.info;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.entity.ResourceType;
import com.waynehays.cloudfilestorage.shared.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Should return mapped resource")
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
    @DisplayName("Should throw exception when resource not found in database")
    void shouldThrowWhenResourceNotFound() {
        // given
        when(metadataService.findOrThrow(USER_ID, "missing.txt"))
                .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

        // when & then
        assertThatThrownBy(() -> service.getInfo(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
