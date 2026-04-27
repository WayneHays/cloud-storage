package com.waynehays.cloudfilestorage.files.operation.delete;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDeletionServiceTest {

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceDeletionService service;

    private static final Long USER_ID = 1L;

    @Nested
    class DeleteFile {

        @Test
        @DisplayName("""
                Should mark for deletion, then delete from storage, then delete metadata from database,
                then release quota
                """)
        void shouldCorrectlyDeleteFile() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    500L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/file.txt")).thenReturn(file);

            // when
            service.delete(USER_ID, "docs/file.txt");

            // then
            InOrder inOrder = inOrder(metadataService, storageService, quotaService);
            inOrder.verify(metadataService).markForDeletion(USER_ID, "docs/file.txt");
            inOrder.verify(storageService).deleteObject(USER_ID, "docs/file.txt");
            inOrder.verify(metadataService).deleteFileByPath(USER_ID, "docs/file.txt");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 500L);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        @DisplayName("""
                Should mark for deletion and sum content sizes,
                then delete from storage, then delete metadata from database, then release quota
                """)
        void shouldMarkAndSumThenDeleteFromStorageThenDeleteMetadataThenReleaseQuota() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.markDirectoryForDeletionAndSumSize(USER_ID, "docs/")).thenReturn(1500L);

            // when
            service.delete(USER_ID, "docs/");

            // then
            InOrder inOrder = inOrder(metadataService, storageService, quotaService);
            inOrder.verify(metadataService).markDirectoryForDeletionAndSumSize(USER_ID, "docs/");
            inOrder.verify(storageService).deleteDirectory(USER_ID, "docs/");
            inOrder.verify(metadataService).deleteDirectoryMetadata(USER_ID, "docs/");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 1500L);
        }

        @Test
        @DisplayName("Should not release quota space when directory is empty")
        void shouldSkipReleaseSpaceWhenDirectoryIsEmpty() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, "empty/", "", "empty",
                    null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "empty/")).thenReturn(dir);
            when(metadataService.markDirectoryForDeletionAndSumSize(USER_ID, "empty/")).thenReturn(0L);

            // when
            service.delete(USER_ID, "empty/");

            // then
            verify(storageService).deleteDirectory(USER_ID, "empty/");
            verify(metadataService).deleteDirectoryMetadata(USER_ID, "empty/");
            verifyNoInteractions(quotaService);
        }

        @Test
        @DisplayName("Should throw exception when metadata not found")
        void shouldThrowWhenResourceNotFound() {
            // given
            when(metadataService.findOrThrow(USER_ID, "missing/"))
                    .thenThrow(new ResourceNotFoundException("Resource not found", "missing/"));

            // when & then
            assertThatThrownBy(() -> service.delete(USER_ID, "missing/"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }
    }
}
