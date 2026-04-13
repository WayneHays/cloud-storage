package com.waynehays.cloudfilestorage.unit.service.scheduler;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.scheduler.cleanup.CleanupService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {
    private static final int LIMIT = 2;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private CleanupService service;

    private ResourceMetadataDto file(Long id, Long userId, String path, long size) {
        return new ResourceMetadataDto(id, userId, path, "parent/", "name", size, ResourceType.FILE);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("Should do nothing when no files marked for deletion")
        void shouldDoNothingWhenEmpty() {
            // given
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of());

            // when
            service.clean(LIMIT);

            // then
            verify(metadataService).findFilesMarkedForDeletion(LIMIT);
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }

        @Test
        @DisplayName("Should delete from storage, release quotas and delete metadata from database")
        void shouldCleanupSuccessfully() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1, f2))
                    .thenReturn(List.of());

            // when
            service.clean(LIMIT);

            // then
            verify(storageService).deleteObjects(10L, List.of("a.txt", "b.txt"));

            ArgumentCaptor<List<SpaceReleaseDto>> releasesCaptor = ArgumentCaptor.captor();
            verify(quotaService).batchDecreaseUsedSpace(releasesCaptor.capture());
            assertThat(releasesCaptor.getValue())
                    .containsExactly(new SpaceReleaseDto(10L, 300L));

            verify(metadataService).deleteByIds(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Should group files by user when deleting from storage and release quotas")
        void shouldGroupByUser() {
            // given
            ResourceMetadataDto u1f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto u1f2 = file(2L, 10L, "b.txt", 200L);
            ResourceMetadataDto u2f1 = file(3L, 20L, "c.txt", 500L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(u1f1, u1f2, u2f1))
                    .thenReturn(List.of());

            // when
            service.clean(LIMIT);

            // then
            verify(storageService).deleteObjects(10L, List.of("a.txt", "b.txt"));
            verify(storageService).deleteObjects(20L, List.of("c.txt"));

            ArgumentCaptor<List<SpaceReleaseDto>> captor = ArgumentCaptor.captor();
            verify(quotaService).batchDecreaseUsedSpace(captor.capture());
            assertThat(captor.getValue())
                    .containsExactlyInAnyOrder(
                            new SpaceReleaseDto(10L, 300L),
                            new SpaceReleaseDto(20L, 500L)
                    );
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("Should loop until batch smaller than limit is returned")
        void shouldLoopUntilPartialBatch() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            ResourceMetadataDto f3 = file(3L, 10L, "c.txt", 300L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1, f2))
                    .thenReturn(List.of(f3))
                    .thenReturn(List.of());

            // when
            service.clean(LIMIT);

            // then
            verify(metadataService, times(2)).findFilesMarkedForDeletion(LIMIT);
            verify(metadataService).deleteByIds(List.of(1L, 2L));
            verify(metadataService).deleteByIds(List.of(3L));
        }

        @Test
        @DisplayName("should stop when empty batch returned")
        void shouldStopOnEmptyBatch() {
            // given
            when(metadataService.findFilesMarkedForDeletion(LIMIT)).thenReturn(List.of());

            // when
            service.clean(LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(LIMIT);
            verifyNoInteractions(storageService);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should skip quotas and metadata when storage delete fails")
        void shouldSkipWhenStorageFails() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1));
            doThrow(new RuntimeException("MinIO error"))
                    .when(storageService).deleteObjects(eq(10L), anyList());

            // when
            service.clean(LIMIT);

            // then
            verify(storageService).deleteObjects(eq(10L), anyList());
            verifyNoInteractions(quotaService);
            verify(metadataService, never()).deleteByIds(anyList());
        }

        @Test
        @DisplayName("should stop pagination loop when storage fails")
        void shouldStopLoopOnStorageFailure() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1, f2));
            doThrow(new RuntimeException("MinIO down"))
                    .when(storageService).deleteObjects(eq(10L), anyList());

            // when
            service.clean(LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(LIMIT);
        }

        @Test
        @DisplayName("should still delete metadata when quota release fails")
        void shouldContinueWhenQuotaReleaseFails() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("Quota service down"))
                    .when(quotaService).batchDecreaseUsedSpace(anyList());

            // when
            service.clean(LIMIT);

            // then
            verify(storageService).deleteObjects(eq(10L), anyList());
            verify(quotaService).batchDecreaseUsedSpace(anyList());
            verify(metadataService).deleteByIds(List.of(1L));
        }

        @Test
        @DisplayName("should stop pagination loop when metadata delete fails")
        void shouldStopLoopOnMetadataDeleteFailure() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenReturn(List.of(f1, f2));
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).deleteByIds(anyList());

            // when
            service.clean(LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(LIMIT);
            verify(storageService).deleteObjects(eq(10L), anyList());
            verify(quotaService).batchDecreaseUsedSpace(anyList());
        }

        @Test
        @DisplayName("should swallow exception from findFilesMarkedForDeletion and exit cleanly")
        void shouldSwallowFindException() {
            // given
            when(metadataService.findFilesMarkedForDeletion(LIMIT))
                    .thenThrow(new RuntimeException("DB down"));

            // when
            service.clean(LIMIT);

            // then
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }
    }
}
