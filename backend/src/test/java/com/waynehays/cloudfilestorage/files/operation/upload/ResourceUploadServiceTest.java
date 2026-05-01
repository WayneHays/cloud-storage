package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
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
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploadServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @Mock
    private BatchInsertMapper batchInsertMapper;

    private ResourceUploadService service;

    @BeforeEach
    void setUp() {
        List<UploadStep> steps = List.of(
                new ValidateStep(metadataService),
                new ReserveQuotaStep(quotaService),
                new StorageUploadStep(storageService, resourceDtoMapper,
                        Executors.newSingleThreadExecutor()),
                new SaveMetadataStep(batchInsertMapper, metadataService),
                new CreateDirectoriesStep(batchInsertMapper, resourceDtoMapper, metadataService)
        );
        service = new ResourceUploadService(steps);
    }

    private UploadObjectDto uploadObject(String storageKey, String fullPath, long size) {
        return new UploadObjectDto(
                storageKey, "file.txt", "file.txt", "", fullPath,
                size, "text/plain", InputStream::nullInputStream);
    }

    @Nested
    class SuccessfulUpload {

        @Test
        @DisplayName("Should execute all steps and return results")
        void shouldExecuteAllStepsAndReturnResults() {
            // given
            UploadObjectDto obj = uploadObject("key-1", "docs/file.txt", 100);
            ResourceDto dto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L)).thenReturn(dto);
            when(metadataService.findMissingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());

            // when
            List<ResourceDto> result = service.upload(USER_ID, List.of(obj));

            // then
            assertThat(result).containsExactly(dto);
            verify(quotaService).reserveSpace(USER_ID, 100L);
            verify(storageService).putObject(eq(USER_ID), eq("key-1"), eq(100L), eq("text/plain"), any());
            verify(metadataService).saveFiles(eq(USER_ID), anyList());
        }
    }

    @Nested
    class ValidationFailure {

        @Test
        @DisplayName("Should throw and not reserve quota when duplicates found")
        void shouldThrowWithoutReservingQuota() {
            // given
            UploadObjectDto obj = uploadObject("key-1", "docs/file.txt", 100);
            when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                    .thenReturn(Set.of("docs/file.txt"));

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(obj)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            verify(quotaService, never()).reserveSpace(anyLong(), anyLong());
            verify(storageService, never()).putObject(any(), any(), anyLong(), any(), any());
        }
    }

    @Nested
    class StorageFailure {

        @Test
        @DisplayName("Should rollback quota when storage upload fails")
        void shouldRollbackQuotaWhenStorageFails() {
            // given
            UploadObjectDto obj = uploadObject("key-1", "docs/file.txt", 100);
            when(metadataService.findExistingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());
            doThrow(new ResourceStorageException("MinIO error"))
                    .when(storageService).putObject(any(), any(), anyLong(), any(), any());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(obj)))
                    .isInstanceOf(ResourceStorageException.class);
            verify(quotaService).releaseSpace(USER_ID, 100L);
            verify(metadataService, never()).saveFiles(anyLong(), anyList());
        }

        @Test
        @DisplayName("Should rollback partially uploaded files when one storage upload fails")
        void shouldRollbackPartialStorageUpload() {
            // given
            UploadObjectDto obj1 = uploadObject("key-1", "docs/a.txt", 100);
            UploadObjectDto obj2 = uploadObject("key-2", "docs/b.txt", 200);
            ResourceDto dto1 = new ResourceDto("docs/", "a.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/a.txt", 100L)).thenReturn(dto1);
            doNothing().when(storageService).putObject(eq(USER_ID), eq("key-1"), eq(100L), eq("text/plain"), any());
            doThrow(new ResourceStorageException("MinIO error"))
                    .when(storageService).putObject(eq(USER_ID), eq("key-2"), eq(200L), eq("text/plain"), any());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(obj1, obj2)))
                    .isInstanceOf(ResourceStorageException.class);

            verify(storageService).deleteObjects(argThat(map ->
                    map.containsKey(USER_ID)
                    && map.get(USER_ID).contains("key-1")
            ));
            verify(quotaService).releaseSpace(USER_ID, 300L);
            verify(metadataService, never()).saveFiles(anyLong(), anyList());
        }
    }

    @Nested
    class MetadataFailure {

        @Test
        @DisplayName("Should rollback storage and quota when metadata save fails")
        void shouldRollbackStorageAndQuotaWhenMetadataFails() {
            // given
            UploadObjectDto obj = uploadObject("key-1", "docs/file.txt", 100);
            ResourceDto dto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);

            when(metadataService.findExistingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());
            when(resourceDtoMapper.fileFromPath("docs/file.txt", 100L)).thenReturn(dto);
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).saveFiles(eq(USER_ID), anyList());

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(obj)))
                    .isInstanceOf(RuntimeException.class);

            verify(storageService).deleteObjects(argThat(map ->
                    map.containsKey(USER_ID)
                    && map.get(USER_ID).contains("key-1")
            ));
            verify(quotaService).releaseSpace(USER_ID, 100L);
        }
    }
}
