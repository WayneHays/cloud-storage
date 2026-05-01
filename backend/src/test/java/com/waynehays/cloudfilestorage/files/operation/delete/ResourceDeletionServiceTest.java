package com.waynehays.cloudfilestorage.files.operation.delete;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Map;

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
        @DisplayName("Should mark for deletion, delete from storage by storageKey, delete metadata, release quota")
        void shouldCorrectlyDeleteFile() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "abc-123", "docs/file.txt", "docs/",
                    "file.txt", 500L, ResourceType.FILE);

            when(metadataService.findByPath(USER_ID, "docs/file.txt")).thenReturn(file);

            // when
            service.delete(USER_ID, "docs/file.txt");

            // then
            InOrder inOrder = inOrder(metadataService, storageService, quotaService);
            inOrder.verify(metadataService).markForDeletion(USER_ID, "docs/file.txt");
            inOrder.verify(storageService).deleteObject(USER_ID, "abc-123");
            inOrder.verify(metadataService).deleteFileByPath(USER_ID, "docs/file.txt");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 500L);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        @DisplayName("Should collect storage keys, delete from storage by keys, delete metadata, release quota")
        void shouldCollectKeysThenDeleteFromStorageThenDeleteMetadataThenReleaseQuota() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, null, "docs/", "",
                    "docs", null, ResourceType.DIRECTORY);
            DeleteDirectoryResult deleteResult = new DeleteDirectoryResult(1500L, List.of("key-1", "key-2"));

            when(metadataService.findByPath(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.markDirectoryForDeletionAndCollectKeys(USER_ID, "docs/")).thenReturn(deleteResult);

            // when
            service.delete(USER_ID, "docs/");

            // then
            InOrder inOrder = inOrder(metadataService, storageService, quotaService);
            inOrder.verify(metadataService).markDirectoryForDeletionAndCollectKeys(USER_ID, "docs/");
            inOrder.verify(storageService).deleteObjects(Map.of(USER_ID, List.of("key-1", "key-2")));
            inOrder.verify(metadataService).deleteDirectoryMetadata(USER_ID, "docs/");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 1500L);
        }

        @Test
        @DisplayName("Should not call storage or release quota when directory is empty")
        void shouldSkipStorageAndQuotaWhenDirectoryIsEmpty() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, null, "empty/", "",
                    "empty", null, ResourceType.DIRECTORY);
            DeleteDirectoryResult deleteResult = new DeleteDirectoryResult(0L, List.of());

            when(metadataService.findByPath(USER_ID, "empty/")).thenReturn(dir);
            when(metadataService.markDirectoryForDeletionAndCollectKeys(USER_ID, "empty/")).thenReturn(deleteResult);

            // when
            service.delete(USER_ID, "empty/");

            // then
            verify(metadataService).deleteDirectoryMetadata(USER_ID, "empty/");
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }

        @Test
        @DisplayName("Should throw exception when metadata not found")
        void shouldThrowWhenResourceNotFound() {
            // given
            when(metadataService.findByPath(USER_ID, "missing/"))
                    .thenThrow(new ResourceNotFoundException("Resource not found", "missing/"));

            // when & then
            assertThatThrownBy(() -> service.delete(USER_ID, "missing/"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(storageService);
            verifyNoInteractions(quotaService);
        }
    }
}