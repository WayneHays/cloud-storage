package com.waynehays.cloudfilestorage.files.cleanup;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {
    private static final int CLEANUP_LIMIT = 2;

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
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(metadataService).findFilesMarkedForDeletion(CLEANUP_LIMIT);
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }

        @Test
        @DisplayName("Should delete from storage, release quotas and delete metadata")
        void shouldCleanupSuccessfully() {
            // given
            ResourceMetadataDto file1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto file2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(file1, file2))
                    .thenReturn(List.of());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<Long, List<String>>> storageCaptor = ArgumentCaptor.forClass(Map.class);
            verify(storageService).deleteObjects(storageCaptor.capture());
            assertThat(storageCaptor.getValue())
                    .containsExactlyEntriesOf(Map.of(10L, List.of("a.txt", "b.txt")));

            ArgumentCaptor<List<SpaceReleaseDto>> quotaCaptor = ArgumentCaptor.captor();
            verify(quotaService).batchReleaseUsedSpace(quotaCaptor.capture());
            assertThat(quotaCaptor.getValue())
                    .containsExactly(new SpaceReleaseDto(10L, 300L));

            verify(metadataService).deleteByIds(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Should group paths by userId for storage deletion")
        void shouldGroupPathsByUserId() {
            // given
            ResourceMetadataDto u1f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto u2f1 = file(2L, 20L, "b.txt", 200L);
            ResourceMetadataDto u1f2 = file(3L, 10L, "c.txt", 300L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(u1f1, u2f1, u1f2))
                    .thenReturn(List.of());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(storageService).deleteObjects(argThat(map ->
                    map.size() == 2
                    && map.get(10L).containsAll(List.of("a.txt", "c.txt"))
                    && map.get(20L).contains("b.txt")
            ));
        }

        @Test
        @DisplayName("Should group quotas by user when releasing space")
        void shouldGroupQuotasByUser() {
            // given
            ResourceMetadataDto u1f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto u1f2 = file(2L, 10L, "b.txt", 200L);
            ResourceMetadataDto u2f1 = file(3L, 20L, "c.txt", 500L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(u1f1, u1f2, u2f1))
                    .thenReturn(List.of());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            ArgumentCaptor<List<SpaceReleaseDto>> captor = ArgumentCaptor.captor();
            verify(quotaService).batchReleaseUsedSpace(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(
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
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(f1, f2))
                    .thenReturn(List.of(f3));

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(metadataService, times(2)).findFilesMarkedForDeletion(CLEANUP_LIMIT);
            verify(metadataService).deleteByIds(List.of(1L, 2L));
            verify(metadataService).deleteByIds(List.of(3L));
        }

        @Test
        @DisplayName("Should stop when empty batch returned")
        void shouldStopOnEmptyBatch() {
            // given
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(CLEANUP_LIMIT);
            verifyNoInteractions(storageService);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should skip quotas and metadata operations when storage delete fails")
        void shouldSkipWhenStorageFails() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(f1));
            doThrow(new RuntimeException("MinIO error"))
                    .when(storageService).deleteObjects(anyMap());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(storageService).deleteObjects(anyMap());
            verifyNoInteractions(quotaService);
            verify(metadataService, never()).deleteByIds(anyList());
        }

        @Test
        @DisplayName("Should stop pagination loop when storage fails")
        void shouldStopLoopOnStorageFailure() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(f1, f2));
            doThrow(new RuntimeException("MinIO error"))
                    .when(storageService).deleteObjects(anyMap());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(CLEANUP_LIMIT);
        }

        @Test
        @DisplayName("Should still delete metadata when quota release fails")
        void shouldContinueWhenQuotaReleaseFails() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(f1))
                    .thenReturn(List.of());
            doThrow(new RuntimeException("Quota service error"))
                    .when(quotaService).batchReleaseUsedSpace(anyList());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(storageService).deleteObjects(anyMap());
            verify(quotaService).batchReleaseUsedSpace(anyList());
            verify(metadataService).deleteByIds(List.of(1L));
        }

        @Test
        @DisplayName("Should stop pagination loop when metadata delete fails")
        void shouldStopLoopOnMetadataDeleteFailure() {
            // given
            ResourceMetadataDto f1 = file(1L, 10L, "a.txt", 100L);
            ResourceMetadataDto f2 = file(2L, 10L, "b.txt", 200L);
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenReturn(List.of(f1, f2));
            doThrow(new RuntimeException("DB error"))
                    .when(metadataService).deleteByIds(anyList());

            // when
            service.processDeletedFiles(CLEANUP_LIMIT);

            // then
            verify(metadataService, times(1)).findFilesMarkedForDeletion(CLEANUP_LIMIT);
            verify(storageService).deleteObjects(anyMap());
            verify(quotaService).batchReleaseUsedSpace(anyList());
        }

        @Test
        @DisplayName("Should propagate exception when findFilesMarkedForDeletion fails")
        void shouldPropagateExceptionFromFind() {
            // given
            when(metadataService.findFilesMarkedForDeletion(CLEANUP_LIMIT))
                    .thenThrow(new RuntimeException("DB error"));

            // when & then
            assertThatThrownBy(() -> service.processDeletedFiles(CLEANUP_LIMIT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }
    }
}
