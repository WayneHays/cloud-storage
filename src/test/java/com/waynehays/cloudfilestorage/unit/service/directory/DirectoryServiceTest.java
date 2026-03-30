package com.waynehays.cloudfilestorage.unit.service.directory;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.directory.DirectoryService;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @Mock
    private ResourceDtoConverter converter;

    @Mock
    private ResourceStorageKeyResolverApi keyResolver;

    @InjectMocks
    private DirectoryService directoryService;

    private static final Long USER_ID = 1L;

    @Nested
    class GetContent {

        @Test
        void shouldReturnDirectoryContents() {
            // given
            String path = "directory/";
            ResourceMetadata child1 = createFileMetadata();
            ResourceMetadata child2 = createDirectoryMetadata();
            ResourceDto dto1 = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);
            ResourceDto dto2 = new ResourceDto("directory/", "sub", null, ResourceType.DIRECTORY);

            when(metadataService.findDirectChildren(USER_ID, path)).thenReturn(List.of(child1, child2));
            when(converter.fromMetadata(child1)).thenReturn(dto1);
            when(converter.fromMetadata(child2)).thenReturn(dto2);

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        void shouldReturnEmptyListForEmptyDirectory() {
            // given
            String path = "empty/";

            when(metadataService.findDirectChildren(USER_ID, path)).thenReturn(List.of());

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrowWhenDirectoryNotFound() {
            // given
            String path = "nonexistent/";
            when(metadataService.findDirectChildren(USER_ID, path))
                    .thenThrow(new ResourceNotFoundException("Resource not found", path));

            // when & then
            assertThatThrownBy(() -> directoryService.getContent(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void shouldCreateDirectoryAndReturnResult() {
            // given
            String path = "directory/subdirectory/";
            String storageKey = "user-1-files/directory/subdirectory/";
            ResourceDto expectedDto = new ResourceDto("directory/", "subdirectory/", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(converter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = directoryService.createDirectory(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(metadataService).throwIfExists(USER_ID, path);
            verify(metadataService).ensureParentExists(USER_ID, path);
            verify(storage).createDirectory(storageKey);
            verify(metadataService).saveDirectory(USER_ID, path);
        }

        @Test
        void shouldCreateRootDirectoryWithoutParentCheck() {
            // given
            String path = "directory/";
            String storageKey = "user-1-files/directory/";
            ResourceDto expectedDto = new ResourceDto("", "directory/", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            when(converter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = directoryService.createDirectory(USER_ID, path);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(metadataService).throwIfExists(USER_ID, path);
            verify(metadataService).ensureParentExists(USER_ID, path);
            verify(storage).createDirectory(storageKey);
            verify(metadataService).saveDirectory(USER_ID, path);
        }

        @Test
        void shouldThrowWhenDirectoryAlreadyExists() {
            // given
            String path = "directory/";

            doThrow(new ResourceAlreadyExistsException("Resource already exists", path))
                    .when(metadataService).throwIfExists(USER_ID, path);

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(storage, never()).createDirectory(any());
            verify(metadataService, never()).saveDirectory(any(), any());
        }

        @Test
        void shouldThrowWhenParentNotFound() {
            // given
            String path = "directory/subdirectory/";

            doThrow(new ResourceNotFoundException("Resource not found", "directory/"))
                    .when(metadataService).ensureParentExists(USER_ID, path);

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(storage, never()).createDirectory(any());
            verify(metadataService, never()).saveDirectory(any(), any());
        }

        @Test
        void shouldNotCallStorageWhenMetadataSaveFails() {
            // given
            String path = "directory/subdirectory/";
            String storageKey = "user-1-files/directory/subdirectory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(storageKey);
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).saveDirectory(USER_ID, path);

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(RuntimeException.class);

            verify(storage).createDirectory(storageKey);
        }
    }

    private ResourceMetadata createFileMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/file.txt");
        metadata.setName(PathUtils.extractFilename("directory/file.txt"));
        metadata.setSize(100L);
        metadata.setType(ResourceType.FILE);
        return metadata;
    }

    private ResourceMetadata createDirectoryMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/sub/");
        metadata.setName(PathUtils.extractFilename("directory/sub/"));
        metadata.setType(ResourceType.DIRECTORY);
        return metadata;
    }
}