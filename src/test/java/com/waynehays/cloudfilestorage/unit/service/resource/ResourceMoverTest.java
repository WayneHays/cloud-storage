package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.constant.Messages;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.mover.ResourceMover;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMoverTest {

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceMetadataServiceApi service;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi converter;

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

            when(service.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);
            when(service.exists(USER_ID, pathTo)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(converter.fileFromPath(pathTo, 100L)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(storage).move(keyFrom, keyTo);
            verify(service).updatePath(USER_ID, pathFrom, pathTo);
        }

        @Test
        void shouldThrowWhenSourceNotFound() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";

            when(service.findOrThrow(USER_ID, pathFrom))
                    .thenThrow(new ResourceNotFoundException(Messages.NOT_FOUND + pathFrom));

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(pathFrom);

            verify(storage, never()).move(any(), any());
        }

        @Test
        void shouldThrowWhenTargetAlreadyExists() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();

            when(service.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);
            when(service.exists(USER_ID, pathTo)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(pathTo);

            verify(storage, never()).move(any(), any());
        }
    }

    @Nested
    class MoveDirectory {

        @Test
        void shouldMoveDirectoryWithContents() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata childFile = new ResourceMetadata();
            childFile.setPath("directory/file.txt");
            childFile.setType(ResourceType.FILE);
            ResourceDto expectedDto = new ResourceDto("", "other", null, ResourceType.DIRECTORY);

            when(service.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(service.exists(USER_ID, pathTo)).thenReturn(false);
            when(service.findDirectoryContent(USER_ID, pathFrom)).thenReturn(List.of(childFile));
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
            verify(storage).createDirectory(keyTo);
            verify(storage).delete(keyFrom);
            verify(storage).move("user-1-files/directory/file.txt", "user-1-files/other/file.txt");
            verify(service).markForDeletionByPrefix(USER_ID, pathFrom);
            verify(service).updateContentPaths(List.of(childFile), pathFrom, pathTo);
        }

        @Test
        void shouldRecreateNestedDirectoryMarkers() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata subDir = new ResourceMetadata();
            subDir.setPath("directory/sub/");
            subDir.setType(ResourceType.DIRECTORY);
            ResourceDto expectedDto = new ResourceDto("", "other", null, ResourceType.DIRECTORY);

            when(service.findOrThrow(USER_ID, pathFrom)).thenReturn(dirMetadata);
            when(service.exists(USER_ID, pathTo)).thenReturn(false);
            when(service.findDirectoryContent(USER_ID, pathFrom)).thenReturn(List.of(subDir));
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
            verify(storage).delete("user-1-files/directory/sub/");
            verify(storage, never()).move("user-1-files/directory/sub/", "user-1-files/other/sub/");
        }

        @Test
        void shouldThrowWhenMovingDirectoryToFile() {
            // given
            String pathFrom = "directory/";
            String pathTo = "file.txt";
            ResourceMetadata metadata = new ResourceMetadata();

            when(service.findOrThrow(USER_ID, pathFrom)).thenReturn(metadata);

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(InvalidMoveException.class);

            verify(storage, never()).move(any(), any());
            verify(storage, never()).createDirectory(any());
        }
    }
}
