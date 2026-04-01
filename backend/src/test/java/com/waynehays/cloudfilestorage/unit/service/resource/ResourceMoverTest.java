package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.mover.ResourceMover;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMoverTest {

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @Mock
    private ResourceStorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverter converter;

    @InjectMocks
    private ResourceMover resourceMover;

    private static final Long USER_ID = 1L;

    @Nested
    class MoveFile {

        @Test
        void shouldMoveFileAndReturnResult() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            String keyFrom = "user-1-files/directory/file.txt";
            String keyTo = "user-1-files/other/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setSize(100L);
            ResourceDto expectedDto = new ResourceDto("other/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(converter.fileFromPath(pathTo, 100L)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(metadataService).throwIfExists(USER_ID, pathTo);
            verify(storage).moveObject(keyFrom, keyTo);
            verify(metadataService).updatePath(USER_ID, pathFrom, pathTo);
        }

        @Test
        void shouldThrowWhenSourceNotFound() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";

            when(metadataService.findOrThrow(USER_ID, pathFrom))
                    .thenThrow(new ResourceNotFoundException("Resource not found", pathFrom));

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(storage, never()).moveObject(any(), any());
        }

        @Test
        void shouldThrowWhenTargetAlreadyExists() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);
            doThrow(new ResourceAlreadyExistsException("Resource already exists", pathTo))
                    .when(metadataService).throwIfExists(USER_ID, pathTo);

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(storage, never()).moveObject(any(), any());
        }

        @Test
        void shouldRenameFileInSameDirectory() {
            // given
            String pathFrom = "directory/old.txt";
            String pathTo = "directory/new.txt";
            String keyFrom = "user-1-files/directory/old.txt";
            String keyTo = "user-1-files/directory/new.txt";
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setSize(50L);
            ResourceDto expectedDto = new ResourceDto("directory/", "new.txt", 50L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(converter.fileFromPath(pathTo, 50L)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(storage).moveObject(keyFrom, keyTo);
            verify(metadataService).updatePath(USER_ID, pathFrom, pathTo);
        }
    }

    @Nested
    class MoveDirectory {

        @Test
        void shouldMoveDirectoryWithFiles() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata childFile = createFileMetadata();
            ResourceDto expectedDto = new ResourceDto("", "other/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(metadataService.findAllByPrefix(USER_ID, pathFrom)).thenReturn(List.of(childFile));
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(keyResolver.resolveKey(USER_ID, "directory/file.txt"))
                    .thenReturn("user-1-files/directory/file.txt");
            when(keyResolver.resolveKey(USER_ID, "other/file.txt"))
                    .thenReturn("user-1-files/other/file.txt");
            when(converter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(metadataService).markForDeletionByPrefix(USER_ID, pathFrom);
            verify(storage).createDirectory(keyTo);
            verify(storage).deleteObject(keyFrom);
            verify(storage).moveObject("user-1-files/directory/file.txt", "user-1-files/other/file.txt");
            verify(metadataService).batchUpdatePaths(List.of(childFile), pathFrom, pathTo);
        }

        @Test
        void shouldRecreateNestedDirectoryMarkers() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata subDir = createDirectoryMetadata();
            ResourceDto expectedDto = new ResourceDto("", "other/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(metadataService.findAllByPrefix(USER_ID, pathFrom)).thenReturn(List.of(subDir));
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(keyResolver.resolveKey(USER_ID, "directory/sub/"))
                    .thenReturn("user-1-files/directory/sub/");
            when(keyResolver.resolveKey(USER_ID, "other/sub/"))
                    .thenReturn("user-1-files/other/sub/");
            when(converter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            verify(storage).createDirectory("user-1-files/other/sub/");
            verify(storage).deleteObject("user-1-files/directory/sub/");
            verify(storage, never()).moveObject("user-1-files/directory/sub/", "user-1-files/other/sub/");
        }

        @Test
        void shouldMoveDirectoryWithMixedContent() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata childFile = createFileMetadata();
            ResourceMetadata childDir = createDirectoryMetadata();
            List<ResourceMetadata> content = List.of(childFile, childDir);
            ResourceDto expectedDto = new ResourceDto("", "other/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(metadataService.findAllByPrefix(USER_ID, pathFrom)).thenReturn(content);
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(keyResolver.resolveKey(USER_ID, "directory/file.txt"))
                    .thenReturn("user-1-files/directory/file.txt");
            when(keyResolver.resolveKey(USER_ID, "other/file.txt"))
                    .thenReturn("user-1-files/other/file.txt");
            when(keyResolver.resolveKey(USER_ID, "directory/sub/"))
                    .thenReturn("user-1-files/directory/sub/");
            when(keyResolver.resolveKey(USER_ID, "other/sub/"))
                    .thenReturn("user-1-files/other/sub/");
            when(converter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            verify(storage).moveObject("user-1-files/directory/file.txt", "user-1-files/other/file.txt");
            verify(storage).createDirectory("user-1-files/other/sub/");
            verify(storage).deleteObject("user-1-files/directory/sub/");
        }

        @Test
        void shouldMoveEmptyDirectory() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceDto expectedDto = new ResourceDto("", "other/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(metadataService.findAllByPrefix(USER_ID, pathFrom)).thenReturn(List.of());
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(converter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(storage).createDirectory(keyTo);
            verify(storage).deleteObject(keyFrom);
            verify(storage, never()).moveObject(any(), any());
            verify(metadataService).batchUpdatePaths(List.of(), pathFrom, pathTo);
        }

        @Test
        void shouldThrowWhenMovingDirectoryToFile() {
            // given
            String pathFrom = "directory/";
            String pathTo = "file.txt";
            ResourceMetadata metadata = new ResourceMetadata();

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(InvalidMoveException.class);

            verify(storage, never()).moveObject(any(), any());
            verify(storage, never()).createDirectory(any());
        }

        @Test
        void shouldFetchContentBeforeMarkingForDeletion() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceDto expectedDto = new ResourceDto("", "other/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(metadataService.findAllByPrefix(USER_ID, pathFrom)).thenReturn(List.of());
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(converter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            InOrder inOrder = inOrder(metadataService);
            inOrder.verify(metadataService).findAllByPrefix(USER_ID, pathFrom);
            inOrder.verify(metadataService).markForDeletionByPrefix(USER_ID, pathFrom);
            inOrder.verify(metadataService).batchUpdatePaths(any(), eq(pathFrom), eq(pathTo));
        }
    }

    private ResourceMetadata createFileMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/file.txt");
        metadata.setSize(100L);
        metadata.setType(ResourceType.FILE);
        return metadata;
    }

    private ResourceMetadata createDirectoryMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/sub/");
        metadata.setType(ResourceType.DIRECTORY);
        return metadata;
    }
}
