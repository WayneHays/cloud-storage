package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.resource.deletion.ResourceDeletionService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
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
    private ResourceStorageService storageService;

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
        void shouldMarkThenDeleteFromStorageThenDeleteMetadataThenReleaseQuota() {
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
            inOrder.verify(metadataService).deleteByPath(USER_ID, "docs/file.txt");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 500L);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        void shouldMarkAndSumThenDeleteFromStorageThenDeleteMetadataThenReleaseQuota() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.markForDeletionAndSumFileSize(USER_ID, "docs/")).thenReturn(1500L);

            // when
            service.delete(USER_ID, "docs/");

            // then
            InOrder inOrder = inOrder(metadataService, storageService, quotaService);
            inOrder.verify(metadataService).markForDeletionAndSumFileSize(USER_ID, "docs/");
            inOrder.verify(storageService).deleteDirectory(USER_ID, "docs/");
            inOrder.verify(metadataService).deleteByPathPrefix(USER_ID, "docs/");
            inOrder.verify(quotaService).releaseSpace(USER_ID, 1500L);
        }

        @Test
        void shouldSkipReleaseSpaceWhenDirectoryIsEmpty() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, "empty/", "", "empty",
                    null, ResourceType.DIRECTORY);

            when(metadataService.findOrThrow(USER_ID, "empty/")).thenReturn(dir);
            when(metadataService.markForDeletionAndSumFileSize(USER_ID, "empty/")).thenReturn(0L);

            // when
            service.delete(USER_ID, "empty/");

            // then
            verify(storageService).deleteDirectory(USER_ID, "empty/");
            verify(metadataService).deleteByPathPrefix(USER_ID, "empty/");
            verifyNoInteractions(quotaService);
        }

        @Test
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
