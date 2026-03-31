package com.waynehays.cloudfilestorage.unit.scheduler;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.cleanup.OrphanResourceCleanerService;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanResourceCleanerServiceTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceStorageKeyResolverApi keyResolver;

    @InjectMocks
    private OrphanResourceCleanerService cleanerService;

    private static final Long USER_ID = 1L;

    @Nested
    class Clean {

        @Test
        void shouldDeleteFromStorage_andThenFromDatabase() {
            // given
            ResourceMetadata orphan = createOrphan(1L, "directory/file.txt");

            when(metadataService.findMarkedForDeletion()).thenReturn(List.of(orphan));
            when(keyResolver.resolveKey(orphan.getUserId(), orphan.getPath()))
                    .thenReturn("user-1-files/directory/file.txt");

            // when
            cleanerService.clean();

            // then
            InOrder inOrder = inOrder(storage, metadataService);
            inOrder.verify(storage).deleteObject("user-1-files/directory/file.txt");
            inOrder.verify(metadataService).deleteById(1L);
        }

        @Test
        void shouldDoNothing_whenNoOrphans() {
            // given
            when(metadataService.findMarkedForDeletion()).thenReturn(List.of());

            // when
            cleanerService.clean();

            // then
            verify(storage, never()).deleteObject(any());
            verify(metadataService, never()).deleteById(any());
        }

        @Test
        void shouldNotDeleteMetadata_whenStorageFails() {
            // given
            ResourceMetadata orphan = createOrphan(1L, "directory/file.txt");

            when(metadataService.findMarkedForDeletion()).thenReturn(List.of(orphan));
            when(keyResolver.resolveKey(orphan.getUserId(), orphan.getPath()))
                    .thenReturn("user-1-files/directory/file.txt");
            doThrow(new ResourceStorageOperationException("MinIO unavailable"))
                    .when(storage).deleteObject("user-1-files/directory/file.txt");

            // when
            cleanerService.clean();

            // then
            verify(metadataService, never()).deleteById(any());
        }

        @Test
        void shouldContinueProcessing_whenOneOrphanFails() {
            // given
            ResourceMetadata orphan1 = createOrphan(1L, "directory/file1.txt");
            ResourceMetadata orphan2 = createOrphan(2L, "directory/file2.txt");

            when(metadataService.findMarkedForDeletion()).thenReturn(List.of(orphan1, orphan2));
            when(keyResolver.resolveKey(orphan1.getUserId(), orphan1.getPath()))
                    .thenReturn("user-1-files/directory/file1.txt");
            when(keyResolver.resolveKey(orphan2.getUserId(), orphan2.getPath()))
                    .thenReturn("user-1-files/directory/file2.txt");
            doThrow(new ResourceStorageOperationException("MinIO unavailable"))
                    .when(storage).deleteObject("user-1-files/directory/file1.txt");

            // when
            cleanerService.clean();

            // then
            verify(metadataService, never()).deleteById(1L);
            verify(storage).deleteObject("user-1-files/directory/file2.txt");
            verify(metadataService).deleteById(2L);
        }

        @Test
        void shouldNotThrow_whenFindMarkedFails() {
            // given
            when(metadataService.findMarkedForDeletion())
                    .thenThrow(new RuntimeException("DB unavailable"));

            // when & then
            assertThatCode(() -> cleanerService.clean()).doesNotThrowAnyException();
        }
    }

    private ResourceMetadata createOrphan(Long id, String path) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setId(id);
        metadata.setUserId(USER_ID);
        metadata.setPath(path);
        metadata.setMarkedForDeletion(true);
        return metadata;
    }
}
