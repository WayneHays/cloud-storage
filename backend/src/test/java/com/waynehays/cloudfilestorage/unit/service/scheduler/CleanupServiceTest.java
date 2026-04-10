package com.waynehays.cloudfilestorage.unit.service.scheduler;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.scheduler.cleanup.CleanupService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private CleanupService service;

    private static final Long USER_ID = 1L;

    @Nested
    class SuccessfulCleanup {

        @Test
        void shouldDeleteStorageThenReleaseQuotasThenDeleteMetadata() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    500L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of(file))
                    .thenReturn(List.of());

            // when
            service.clean(50);

            // then
            InOrder inOrder = inOrder(storageService, quotaService, metadataService);
            inOrder.verify(storageService).deleteObjects(USER_ID, List.of("docs/file.txt"));
            inOrder.verify(quotaService).batchDecreaseUsedSpace(anyList());
            inOrder.verify(metadataService).deleteByIds(List.of(1L));
        }

        @Test
        void shouldProcessMultipleBatches() {
            // given
            ResourceMetadataDto file1 = new ResourceMetadataDto(
                    1L, USER_ID, "a.txt", "", "a.txt",
                    100L, ResourceType.FILE);
            ResourceMetadataDto file2 = new ResourceMetadataDto(
                    2L, USER_ID, "b.txt", "", "b.txt",
                    200L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(1))
                    .thenReturn(List.of(file1))
                    .thenReturn(List.of(file2))
                    .thenReturn(List.of());

            // when
            service.clean(1);

            // then
            verify(storageService, times(2)).deleteObjects(eq(USER_ID), anyList());
            verify(metadataService, times(2)).deleteByIds(anyList());
        }

        @Test
        void shouldDoNothingWhenNoFilesMarked() {
            // given
            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of());

            // when
            service.clean(50);

            // then
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }

        @Test
        void shouldGroupByUserIdForStorageAndQuota() {
            // given
            ResourceMetadataDto file1 = new ResourceMetadataDto(
                    1L, 10L, "a.txt", "", "a.txt",
                    100L, ResourceType.FILE);
            ResourceMetadataDto file2 = new ResourceMetadataDto(
                    2L, 20L, "b.txt", "", "b.txt",
                    200L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of(file1, file2))
                    .thenReturn(List.of());

            // when
            service.clean(50);

            // then
            verify(storageService).deleteObjects(10L, List.of("a.txt"));
            verify(storageService).deleteObjects(20L, List.of("b.txt"));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldStopBatchWhenStorageFails() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "file.txt", "", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of(file));
            doThrow(new ResourceStorageOperationException("MinIO error"))
                    .when(storageService).deleteObjects(USER_ID, List.of("file.txt"));

            // when
            service.clean(50);

            // then
            verifyNoInteractions(quotaService);
            verify(metadataService, never()).deleteByIds(anyList());
        }

        @Test
        void shouldContinueToDeleteMetadataWhenQuotaReleaseFails() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "file.txt", "", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of(file))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("DB error"))
                    .when(quotaService).batchDecreaseUsedSpace(anyList());

            // when
            service.clean(50);

            // then
            verify(metadataService).deleteByIds(List.of(1L));
        }

        @Test
        void shouldStopBatchWhenMetadataDeleteFails() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "file.txt", "", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findFilesMarkedForDeletion(50))
                    .thenReturn(List.of(file));
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).deleteByIds(List.of(1L));

            // when
            service.clean(50);

            // then
            verify(storageService).deleteObjects(USER_ID, List.of("file.txt"));
            verify(quotaService).batchDecreaseUsedSpace(anyList());
        }
    }
}
