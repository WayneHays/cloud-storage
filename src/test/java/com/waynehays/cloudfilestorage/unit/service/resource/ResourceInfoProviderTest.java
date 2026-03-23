package com.waynehays.cloudfilestorage.unit.service.resource;


import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.constant.Messages;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.infoprovider.ResourceInfoProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceInfoProviderTest {

    @Mock
    private ResourceMetadataServiceApi service;

    @Mock
    private ResourceDtoConverterApi converter;

    @InjectMocks
    private ResourceInfoProvider resourceInfoProvider;

    private static final Long USER_ID = 1L;

    @Nested
    class GetFileInfo {

        @Test
        void shouldReturnFileInfo() {
            // given
            String path = "directory/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setSize(100L);
            ResourceDto expectedDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(service.findOrThrow(USER_ID, path)).thenReturn(metadata);
            when(converter.fileFromPath(path, 100L)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceInfoProvider.getInfo(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        void shouldThrowWhenFileNotFound() {
            // given
            String path = "directory/file.txt";

            when(service.findOrThrow(USER_ID, path))
                    .thenThrow(new ResourceNotFoundException(Messages.NOT_FOUND + path));

            // when & then
            assertThatThrownBy(() -> resourceInfoProvider.getInfo(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);
        }
    }

    @Nested
    class GetDirectoryInfo {

        @Test
        void shouldReturnDirectoryInfo() {
            // given
            String path = "directory/subdirectory/";
            ResourceMetadata metadata = new ResourceMetadata();
            ResourceDto expectedDto = new ResourceDto("directory/", "subdirectory", null, ResourceType.DIRECTORY);

            when(service.findOrThrow(USER_ID, path)).thenReturn(metadata);
            when(converter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceInfoProvider.getInfo(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(converter, never()).fileFromPath(any(), any());
        }
    }
}
