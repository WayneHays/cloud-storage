package com.waynehays.cloudfilestorage.unit.service.directory;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.service.directory.DirectoryService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    private FileStorageApi fileStorage;

    @Mock
    private ResourceDtoConverterApi dtoConverter;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @InjectMocks
    private DirectoryService directoryService;

    @Nested
    class GetContent {

        @Test
        void shouldReturnDirectoryContents() {
            // given
            String path = "directory/";
            String prefix = "user-1-files/directory/";
            String childKey = "user-1-files/directory/file.txt";
            MetaData childMetaData = new MetaData(childKey, "file.txt", 100L, "text/plain", false);
            MetaData selfMetaData = new MetaData(prefix, "directory/", 0L, "text/plain", false);
            ResourceDto expectedDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(prefix);
            when(fileStorage.getList(prefix)).thenReturn(List.of(selfMetaData, childMetaData));
            when(keyResolver.extractPath(USER_ID, childKey)).thenReturn("directory/file.txt");
            when(dtoConverter.convert(childMetaData, "directory/file.txt")).thenReturn(expectedDto);

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        void shouldFilterOutSelfMarker() {
            // given
            String path = "directory/";
            String prefix = "user-1-files/directory/";
            MetaData selfMetaData = new MetaData(prefix, "directory/", 0L, "text/plain", true);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(prefix);
            when(fileStorage.getList(prefix)).thenReturn(List.of(selfMetaData));

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).isEmpty();
            verify(dtoConverter, never()).convert(any(), any());
        }

        @Test
        void shouldThrowWhenDirectoryNotFoundAndContentEmpty() {
            // given
            String path = "nonexistent/";
            String prefix = "user-1-files/nonexistent/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(prefix);
            when(fileStorage.getList(prefix)).thenReturn(List.of());
            when(fileStorage.getMetaData(prefix)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> directoryService.getContent(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);
        }

        @Test
        void shouldNotCheckExistenceWhenContentIsPresent() {
            // given
            String path = "directory/";
            String prefix = "user-1-files/directory/";
            String childKey = "user-1-files/directory/file.txt";
            MetaData childMetaData = new MetaData(childKey, "file.txt", 100L, "text/plain", false);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(prefix);
            when(fileStorage.getList(prefix)).thenReturn(List.of(childMetaData));
            when(keyResolver.extractPath(USER_ID, childKey)).thenReturn("directory/file.txt");
            when(dtoConverter.convert(any(), any())).thenReturn(
                    new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE));

            // when
            directoryService.getContent(USER_ID, path);

            // then
            verify(fileStorage, never()).getMetaData(any());
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void shouldCreateDirectoryAndReturnResult() {
            // given
            String path = "directory/subdirectory/";
            String storageKey = "user-1-files/directory/subdirectory/";
            String parentPath = "directory/";
            String parentKey = "user-1-files/directory/";
            MetaData parentMetaData = new MetaData(parentKey, "directory", null, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("directory/", "subdirectory", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, parentPath)).thenReturn(parentKey);
            when(fileStorage.getMetaData(parentKey)).thenReturn(Optional.of(parentMetaData));
            when(dtoConverter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = directoryService.createDirectory(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(fileStorage).createDirectory(storageKey);
        }

        @Test
        void shouldCreateRootDirectoryWithoutParentCheck() {
            // given
            String path = "directory/";
            String storageKey = "user-1-files/directory/";
            ResourceDto expectedDto = new ResourceDto("", "directory", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(dtoConverter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = directoryService.createDirectory(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(fileStorage).createDirectory(storageKey);
            verify(fileStorage, never()).getMetaData(any());
        }

        @Test
        void shouldThrowWhenDirectoryAlreadyExists() {
            // given
            String path = "directory/";
            String storageKey = "user-1-files/directory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(path);

            verify(fileStorage, never()).createDirectory(any());
        }

        @Test
        void shouldThrowWhenParentDirectoryNotFound() {
            // given
            String path = "directory/subdirectory/";
            String storageKey = "user-1-files/directory/subdirectory/";
            String parentPath = "directory/";
            String parentKey = "user-1-files/directory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, parentPath)).thenReturn(parentKey);
            when(fileStorage.getMetaData(parentKey)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(parentPath);

            verify(fileStorage, never()).createDirectory(any());
        }
    }
}
