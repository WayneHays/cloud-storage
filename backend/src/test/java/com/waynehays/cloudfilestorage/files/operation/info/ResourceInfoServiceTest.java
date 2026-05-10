package com.waynehays.cloudfilestorage.files.operation.info;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
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
    private ResourceResponseMapper mapper;

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
                1L, USER_ID, "storage-key","docs/file.txt", "docs/", "file.txt",
                100L, ResourceType.FILE);
        ResourceResponse expected = new ResourceResponse("docs/", "file.txt", 100L, ResourceType.FILE);

        when(metadataService.findByPath(USER_ID, "docs/file.txt")).thenReturn(metadata);
        when(mapper.fromResourceMetadataDto(metadata)).thenReturn(expected);

        // when
        ResourceResponse result = service.getInfo(USER_ID, "docs/file.txt");

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw exception when resource not found in database")
    void shouldThrowWhenResourceNotFound() {
        // given
        when(metadataService.findByPath(USER_ID, "missing.txt"))
                .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

        // when & then
        assertThatThrownBy(() -> service.getInfo(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
