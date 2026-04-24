package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.entity.ResourceType;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.shared.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.shared.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.shared.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.storage.service.ResourceStorageServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMoveServiceTest {

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceDtoMapper mapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    private ResourceMoveService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ExecutorService moveExecutor = Executors.newSingleThreadExecutor();
        List<MoveStep> steps = List.of(
                new ValidateMoveStep(metadataService),
                new MoveStorageStep(storageService, metadataService, moveExecutor),
                new MoveMetadataStep(metadataService)
        );
        service = new ResourceMoveService(steps, metadataService, mapper);
    }

    @Nested
    class MoveFile {

        @Test
        @DisplayName("Should move resource in storage and then update metadata in database")
        void shouldMoveStorageThenUpdateMetadata() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);
            ResourceDto expected = new ResourceDto("images/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(file);
            when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);
            when(mapper.fileFromPath("images/file.txt", 100L)).thenReturn(expected);

            // when
            ResourceDto result = service.move(USER_ID, "docs/file.txt", "images/file.txt");

            // then
            InOrder inOrder = inOrder(storageService, metadataService);
            inOrder.verify(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");
            inOrder.verify(metadataService).moveMetadata(USER_ID, "docs/file.txt", "images/file.txt");
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class MoveDirectory {

        @Test
        @DisplayName("Should move all files in storage and then update metadata in database")
        void shouldMoveAllFilesThenUpdateMetadata() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file = new ResourceMetadataDto(
                    2L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);
            ResourceDto expected = new ResourceDto("", "images/", null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(false);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of(file));
            when(mapper.directoryFromPath("images/")).thenReturn(expected);

            // when
            ResourceDto result = service.move(USER_ID, "docs/", "images/");

            // then
            verify(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");
            verify(metadataService).moveMetadata(USER_ID, "docs/", "images/");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should rollback already moved files in storage when one storage move fails")
        void shouldRollbackOnStorageFailure() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file1 = new ResourceMetadataDto(
                    2L, USER_ID, "docs/a.txt", "docs/", "a.txt",
                    100L, ResourceType.FILE);
            ResourceMetadataDto file2 = new ResourceMetadataDto(
                    3L, USER_ID, "docs/b.txt", "docs/", "b.txt",
                    200L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(false);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/"))
                    .thenReturn(List.of(file1, file2));
            doNothing().when(storageService).moveObject(USER_ID, "docs/a.txt", "images/a.txt");
            doThrow(new ResourceStorageOperationException("MinIO error"))
                    .when(storageService).moveObject(USER_ID, "docs/b.txt", "images/b.txt");

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "images/"))
                    .isInstanceOf(ResourceStorageOperationException.class);
            verify(storageService).moveObject(USER_ID, "images/a.txt", "docs/a.txt");
            verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw and not update metadata when storage operation fails")
        void shouldThrowAndNotUpdateMetadataOnStorageFailure() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(file);
            when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);
            doThrow(new ResourceStorageOperationException("MinIO error"))
                    .when(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/file.txt", "images/file.txt"))
                    .isInstanceOf(ResourceStorageOperationException.class);

            // файл не переместился — rollback не нужен, метаданные не тронуты
            verify(storageService, never()).moveObject(USER_ID, "images/file.txt", "docs/file.txt");
            verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should rollback storage move when metadata update fails")
        void shouldRollbackStorageWhenMetadataFails() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(file);
            when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).moveMetadata(USER_ID, "docs/file.txt", "images/file.txt");

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/file.txt", "images/file.txt"))
                    .isInstanceOf(RuntimeException.class);
            verify(storageService).moveObject(USER_ID, "images/file.txt", "docs/file.txt");
        }

        @Test
        @DisplayName("Should continue rollback when one rollback step throws")
        void shouldContinueRollbackWhenOneRollbackStepThrows() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file = new ResourceMetadataDto(
                    2L, USER_ID, "docs/a.txt", "docs/", "a.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(false);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of(file));
            doThrow(new ResourceStorageOperationException("DB error"))
                    .when(metadataService).moveMetadata(USER_ID, "docs/", "images/");
            doThrow(new RuntimeException("Rollback also failed"))
                    .when(storageService).moveObject(USER_ID, "images/a.txt", "docs/a.txt");

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "images/"))
                    .isInstanceOf(ResourceStorageOperationException.class);
        }

        @Test
        @DisplayName("Should move empty directory without touching storage")
        void shouldMoveEmptyDirectory() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(false);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of());
            when(mapper.directoryFromPath("images/")).thenReturn(
                    new ResourceDto("", "images/", null, ResourceType.DIRECTORY));

            // when
            service.move(USER_ID, "docs/", "images/");

            // then
            verifyNoInteractions(storageService);
            verify(metadataService).moveMetadata(USER_ID, "docs/", "images/");
        }
    }

    @Nested
    class Validation {

        @Test
        @DisplayName("Should throw when moving directory to a file path")
        void shouldThrowWhenMovingDirectoryToFile() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "file.txt"))
                    .isInstanceOf(InvalidMoveException.class);
            verify(storageService, never()).moveObject(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw when moving directory into itself")
        void shouldThrowWhenMovingDirectoryIntoItself() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "docs/sub/"))
                    .isInstanceOf(InvalidMoveException.class);
            verify(storageService, never()).moveObject(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw when target path already exists")
        void shouldThrowWhenTargetAlreadyExists() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);
            when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(file);
            when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/file.txt", "images/file.txt"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            verify(storageService, never()).moveObject(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw when source resource does not exist")
        void shouldThrowWhenResourceNotFound() {
            // given
            when(metadataService.findOrThrow(USER_ID, "missing.txt"))
                    .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "missing.txt", "new.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(storageService, never()).moveObject(anyLong(), anyString(), anyString());
        }
    }
}
