package com.waynehays.cloudfilestorage.unit.service.resource;


import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.service.resource.infoprovider.ResourceInfoProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ResourceInfoProviderTest {
    private static final Long USER_ID = 1L;

    @Mock
    private FileStorageApi fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi dtoConverter;

    @InjectMocks
    private ResourceInfoProvider infoProvider;

    @Nested
    class GetFileInfo {

        @Test
        void shouldReturnFileInfo() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            MetaData metaData = new MetaData(objectKey, "file.txt", 100L, "text/plain", false);
            ResourceDto expectedDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.getMetaData(objectKey)).thenReturn(Optional.of(metaData));
            when(dtoConverter.convert(metaData, path)).thenReturn(expectedDto);

            // when
            ResourceDto result = infoProvider.getInfo(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        void shouldThrowWhenFileNotFound() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.getMetaData(objectKey)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> infoProvider.getInfo(USER_ID, path))
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
            String objectKey = "user-1-files/directory/subdirectory/";
            MetaData metaData = new MetaData(objectKey, "subdirectory", null, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("directory/", "subdirectory", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.getMetaData(objectKey)).thenReturn(Optional.of(metaData));
            when(dtoConverter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = infoProvider.getInfo(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(dtoConverter, never()).convert(any(), any());
        }

        @Test
        void shouldThrowWhenDirectoryNotFound() {
            // given
            String path = "directory/subdirectory/";
            String objectKey = "user-1-files/directory/subdirectory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.getMetaData(objectKey)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> infoProvider.getInfo(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);
        }
    }
}
