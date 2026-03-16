package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.service.resource.mover.ResourceMover;
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
class ResourceMoverTest {
    private static final Long USER_ID = 1L;

    @Mock
    private FileStorageApi fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi dtoConverter;

    @InjectMocks
    private ResourceMover resourceMover;

    @Nested
    class MoveFile {

        @Test
        void shouldMoveFileAndReturnResult() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            String keyFrom = "user-1-files/directory/file.txt";
            String keyTo = "user-1-files/other/file.txt";
            MetaData metaData = new MetaData(keyFrom, "file.txt", 100L, "text/plain", false);
            ResourceDto expectedDto = new ResourceDto("other/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.of(metaData));
            when(fileStorage.exists(keyTo)).thenReturn(false);
            when(dtoConverter.convert(metaData, pathTo)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(fileStorage).move(keyFrom, keyTo);
        }

        @Test
        void shouldThrowWhenSourceFileNotFound() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            String keyFrom = "user-1-files/directory/file.txt";
            String keyTo = "user-1-files/other/file.txt";

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(pathFrom);

            verify(fileStorage, never()).move(any(), any());
        }

        @Test
        void shouldThrowWhenTargetAlreadyExists() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";
            String keyFrom = "user-1-files/directory/file.txt";
            String keyTo = "user-1-files/other/file.txt";
            MetaData metaData = new MetaData(keyFrom, "file.txt", 100L, "text/plain", false);

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.of(metaData));
            when(fileStorage.exists(keyTo)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(pathTo);

            verify(fileStorage, never()).move(any(), any());
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
            String childKey = "user-1-files/directory/file.txt";
            String newChildKey = "user-1-files/other/file.txt";
            MetaData dirMetaData = new MetaData(keyFrom, "directory", null, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("", "other", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.of(dirMetaData));
            when(fileStorage.exists(keyTo)).thenReturn(false);
            when(fileStorage.getListRecursive(keyFrom)).thenReturn(List.of(
                    new MetaData(childKey, "file.txt", 100L, "text/plain", false)
            ));
            when(dtoConverter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(fileStorage).move(keyFrom, keyTo);
            verify(fileStorage).move(childKey, newChildKey);
        }

        @Test
        void shouldMoveEmptyDirectory() {
            // given
            String pathFrom = "directory/";
            String pathTo = "other/";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/other/";
            MetaData dirMetaData = new MetaData(keyFrom, "directory", null, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("", "other", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.of(dirMetaData));
            when(fileStorage.exists(keyTo)).thenReturn(false);
            when(fileStorage.getListRecursive(keyFrom)).thenReturn(List.of());
            when(dtoConverter.directoryFromPath(pathTo)).thenReturn(expectedDto);

            // when
            ResourceDto result = resourceMover.move(USER_ID, pathFrom, pathTo);

            // then
            assertThat(result).isEqualTo(expectedDto);
            verify(fileStorage).move(keyFrom, keyTo);
        }

        @Test
        void shouldThrowWhenMovingDirectoryToFile() {
            // given
            String pathFrom = "directory/";
            String pathTo = "file.txt";
            String keyFrom = "user-1-files/directory/";
            String keyTo = "user-1-files/file.txt";
            MetaData dirMetaData = new MetaData(keyFrom, "directory", null, "text/plain", true);

            when(keyResolver.resolveKey(USER_ID, pathFrom)).thenReturn(keyFrom);
            when(keyResolver.resolveKey(USER_ID, pathTo)).thenReturn(keyTo);
            when(fileStorage.getMetaData(keyFrom)).thenReturn(Optional.of(dirMetaData));

            // when & then
            assertThatThrownBy(() -> resourceMover.move(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(InvalidMoveException.class);

            verify(fileStorage, never()).move(any(), any());
        }
    }
}
