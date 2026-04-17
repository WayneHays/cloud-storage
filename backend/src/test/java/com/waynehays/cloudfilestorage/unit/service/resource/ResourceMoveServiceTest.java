package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.move.ResourceMoveService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
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
    private ResourceStorageService storageService;

    @Mock
    private ResourceDtoMapper mapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    private ResourceMoveService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ExecutorService moveExecutor = Executors.newSingleThreadExecutor();
        service = new ResourceMoveService(storageService, moveExecutor, mapper, metadataService);
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
        @DisplayName("Should rollback when move resource in storage fails")
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
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/"))
                    .thenReturn(List.of(file1, file2));

            doNothing().when(storageService).moveObject(USER_ID, "docs/a.txt", "images/a.txt");
            doThrow(new ResourceStorageOperationException("MinIO error"))
                    .when(storageService).moveObject(USER_ID, "docs/b.txt", "images/b.txt");

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "images/"))
                    .isInstanceOf(ResourceStorageOperationException.class);
            verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
        }
    }

    @Nested
    class Validation {

        @Test
        @DisplayName("Should throw exception when move directory to file ")
        void shouldThrowWhenMovingDirectoryToFile() {
            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "file.txt"))
                    .isInstanceOf(InvalidMoveException.class);
            verifyNoInteractions(metadataService);
        }

        @Test
        @DisplayName("Should throw exception when move directory into itself")
        void shouldThrowWhenMovingDirectoryIntoItself() {
            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "docs/", "docs/sub/"))
                    .isInstanceOf(InvalidMoveException.class);
            verifyNoInteractions(metadataService);
        }

        @Test
        @DisplayName("Should throw exception when metadata not found in database")
        void shouldThrowWhenResourceNotFound() {
            // given
            when(metadataService.findOrThrow(USER_ID, "missing.txt"))
                    .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

            // when & then
            assertThatThrownBy(() -> service.move(USER_ID, "missing.txt", "new.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(storageService);
        }
    }
}
