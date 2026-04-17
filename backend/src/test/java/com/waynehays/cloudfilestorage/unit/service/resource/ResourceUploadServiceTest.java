package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRowDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.mapper.ResourceRowMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.resource.upload.ResourceUploadService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploadServiceTest {

    @Mock
    private ResourceRowMapper resourceRowMapper;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @Mock
    private ResourceStorageService storageService;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    private ResourceUploadService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
        service = new ResourceUploadService(
                resourceRowMapper, resourceDtoMapper, uploadExecutor,
                storageService, quotaService, metadataService);
    }

    @Nested
    class SuccessfulUpload {

        @Test
        @DisplayName("Should reserve quota, upload resource and save metadata")
        void shouldUploadResource() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/file.txt", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of("docs/"));
            when(resourceDtoMapper.directoriesFromPaths(anySet()))
                    .thenReturn(List.of());
            when(resourceRowMapper.toDirectoryRows(anySet()))
                    .thenReturn(List.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L))
                    .thenReturn(fileDto);

            // when
            List<ResourceDto> result = service.upload(USER_ID, List.of(object));

            // then
            verify(quotaService).reserveSpace(USER_ID, 100L);
            verify(storageService).putObject(USER_ID, object);
            verify(metadataService).saveFiles(eq(USER_ID), anyList());
            assertThat(result).contains(fileDto);
        }

        @Test
        @DisplayName("Should upload multiple files and return all results")
        void shouldUploadMultipleFiles() {
            // given
            UploadObjectDto object1 = new UploadObjectDto(
                    "a.txt", "a.txt", "docs/", "docs/a.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            UploadObjectDto object2 = new UploadObjectDto(
                    "b.txt", "b.txt", "work/", "work/b.txt",
                    200L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto1 = new ResourceDto("docs/", "a.txt", 100L, ResourceType.FILE);
            ResourceDto fileDto2 = new ResourceDto("work/", "b.txt", 200L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/a.txt", 100L))
                    .thenReturn(fileDto1);
            when(resourceDtoMapper.fileFromPath("work/b.txt", 200L))
                    .thenReturn(fileDto2);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());

            // when
            List<ResourceDto> result = service.upload(USER_ID, List.of(object1, object2));

            // then
            verify(quotaService).reserveSpace(USER_ID, 300L);
            verify(storageService).putObject(USER_ID, object1);
            verify(storageService).putObject(USER_ID, object2);
            assertThat(result).containsExactlyInAnyOrder(fileDto1, fileDto2);
        }

        @Test
        @DisplayName("Should skip directory creation when uploading file to root")
        void shouldSkipDirectoryCreationWhenUploadingToRoot() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "", "file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());

            // when
            List<ResourceDto> result = service.upload(USER_ID, List.of(object));

            // then
            verify(metadataService, never()).findMissingPaths(anyLong(), anySet());
            verify(metadataService, never()).saveDirectories(anyLong(), anyList());
            assertThat(result).containsExactly(fileDto);
        }
    }

    @Nested
    class Validation {

        @Test
        @DisplayName("Should throw exception when duplicate paths in upload request")
        void shouldThrowWhenDuplicate() {
            // given
            UploadObjectDto object1 = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            UploadObjectDto object2 = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    200L, "text/plain", InputStream::nullInputStream);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object1, object2)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            verifyNoInteractions(quotaService);
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("Should throw exception when resource already exists")
        void shouldThrowWhenExists() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of("docs/file.txt"));

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            verifyNoInteractions(quotaService);
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("Should throw exception when not enough storage space")
        void shouldThrowWhenNotEnoughQuota() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            doThrow(new ResourceStorageLimitException("Not enough space", 100L, 50L))
                    .when(quotaService).reserveSpace(USER_ID, 100L);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(ResourceStorageLimitException.class);
            verifyNoInteractions(storageService);
        }
    }

    @Nested
    class Rollback {

        @Test
        @DisplayName("Should rollback storage when storage save fails")
        void shouldRollbackOnStorageFailure() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            doThrow(new ResourceStorageOperationException("MinIO error"))
                    .when(storageService).putObject(USER_ID, object);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(ResourceStorageOperationException.class);
            verify(quotaService).releaseSpace(USER_ID, 100L);
        }

        @Test
        @DisplayName("Should rollback metadata and storage when metadata save fails")
        void shouldRollbackOnMetadataFailure() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).saveFiles(eq(USER_ID), anyList());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(RuntimeException.class);
            verify(storageService).deleteObjects(anyList());
            verify(quotaService).releaseSpace(USER_ID, 100L);
        }

        @Test
        @DisplayName("Should rollback files, storage and quota when directory save fails")
        void shouldRollbackWhenDirectorySaveFails() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of("docs/"));
            when(resourceRowMapper.toDirectoryRows(anySet()))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).saveDirectories(eq(USER_ID), anyList());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(RuntimeException.class);
            verify(metadataService).deleteByPaths(eq(USER_ID), anyList());
            verify(storageService).deleteObjects(anyList());
            verify(quotaService).releaseSpace(USER_ID, 100L);
        }

        @Test
        @DisplayName("Should not release quota when reservation itself failed")
        void shouldNotReleaseQuotaWhenReservationFailed() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            doThrow(new ResourceStorageLimitException("Not enough space", 100L, 50L))
                    .when(quotaService).reserveSpace(USER_ID, 100L);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(ResourceStorageLimitException.class);
            verify(quotaService, never()).releaseSpace(anyLong(), anyLong());
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("Should continue rollback when storage cleanup fails")
        void shouldContinueRollbackWhenStorageCleanupFails() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).saveFiles(eq(USER_ID), anyList());
            doThrow(new RuntimeException("Storage cleanup also failed"))
                    .when(storageService).deleteObjects(anyList());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(object)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
            verify(quotaService).releaseSpace(USER_ID, 100L);
        }

    }

    @Nested
    class CreateMissingDirectories {

        @Test
        @DisplayName("Should save only missing directories")
        void shouldSaveOnlyMissingDirectories() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/sub/", "docs/sub/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/sub/", "file.txt", 100L, ResourceType.FILE);
            List<DirectoryRowDto> missingRows = List.of(
                    new DirectoryRowDto("docs/sub/", "docs/", "sub")
            );

            when(resourceDtoMapper.fileFromPath("docs/sub/file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of("docs/sub/"));
            when(resourceRowMapper.toDirectoryRows(Set.of("docs/sub/")))
                    .thenReturn(missingRows);
            when(resourceDtoMapper.directoriesFromPaths(anySet()))
                    .thenReturn(List.of());

            // when
            service.upload(USER_ID, List.of(object));

            // then
            verify(resourceRowMapper).toDirectoryRows(Set.of("docs/sub/"));
            verify(metadataService).saveDirectories(USER_ID, missingRows);
        }

        @Test
        @DisplayName("Should not save directories when all already exists")
        void shouldNotSaveDirectoriesWhenAllAlreadyExists() {
            // given
            UploadObjectDto object = new UploadObjectDto(
                    "file.txt", "file.txt", "docs/", "docs/file.txt",
                    100L, "text/plain", InputStream::nullInputStream);
            ResourceDto fileDto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L))
                    .thenReturn(fileDto);
            when(resourceRowMapper.toFileRows(anyList()))
                    .thenReturn(List.of());
            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());
            when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of());  // все директории уже есть

            // when
            service.upload(USER_ID, List.of(object));

            // then
            verify(metadataService, never()).saveDirectories(anyLong(), anyList());
            verify(resourceRowMapper, never()).toDirectoryRows(anySet());
        }
    }
}
