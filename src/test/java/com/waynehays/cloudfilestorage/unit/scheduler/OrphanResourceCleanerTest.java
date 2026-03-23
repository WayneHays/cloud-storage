package com.waynehays.cloudfilestorage.unit.scheduler;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.sheduler.OrphanResourceCleaner;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
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
class OrphanResourceCleanerTest {

    @Mock
    private ResourceMetadataServiceApi service;

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @InjectMocks
    private OrphanResourceCleaner cleaner;

    private static final Long USER_ID = 1L;

    @Nested
    class Clean {

        @Test
        void shouldDeleteFromStorage_andThenFromDatabase() {
            // given
            ResourceMetadata orphan = createOrphan(1L, "directory/file.txt");

            when(service.findMarkedForDeletion()).thenReturn(List.of(orphan));
            when(keyResolver.resolveKey(orphan.getUserId(), orphan.getPath()))
                    .thenReturn("user-1-files/directory/file.txt");

            // when
            cleaner.clean();

            // then
            InOrder inOrder = inOrder(storage, service);
            inOrder.verify(storage).delete("user-1-files/directory/file.txt");
            inOrder.verify(service).deleteById(1L);
        }

        @Test
        void shouldDoNothing_whenNoOrphans() {
            // given
            when(service.findMarkedForDeletion()).thenReturn(List.of());

            // when
            cleaner.clean();

            // then
            verify(storage, never()).delete(any());
            verify(service, never()).deleteById(any());
        }

        @Test
        void shouldNotDeleteMetadata_whenStorageFails() {
            // given
            ResourceMetadata orphan = createOrphan(1L, "directory/file.txt");

            when(service.findMarkedForDeletion()).thenReturn(List.of(orphan));
            when(keyResolver.resolveKey(orphan.getUserId(), orphan.getPath()))
                    .thenReturn("user-1-files/directory/file.txt");
            doThrow(new FileStorageException("MinIO unavailable"))
                    .when(storage).delete("user-1-files/directory/file.txt");

            // when
            cleaner.clean();

            // then
            verify(service, never()).deleteById(any());
        }

        @Test
        void shouldContinueProcessing_whenOneOrphanFails() {
            // given
            ResourceMetadata orphan1 = createOrphan(1L, "directory/file1.txt");
            ResourceMetadata orphan2 = createOrphan(2L, "directory/file2.txt");

            when(service.findMarkedForDeletion()).thenReturn(List.of(orphan1, orphan2));
            when(keyResolver.resolveKey(orphan1.getUserId(), orphan1.getPath()))
                    .thenReturn("user-1-files/directory/file1.txt");
            when(keyResolver.resolveKey(orphan2.getUserId(), orphan2.getPath()))
                    .thenReturn("user-1-files/directory/file2.txt");
            doThrow(new FileStorageException("MinIO unavailable"))
                    .when(storage).delete("user-1-files/directory/file1.txt");

            // when
            cleaner.clean();

            // then
            verify(service, never()).deleteById(1L);
            verify(storage).delete("user-1-files/directory/file2.txt");
            verify(service).deleteById(2L);
        }

        @Test
        void shouldNotThrow_whenFindMarkedFails() {
            // given
            when(service.findMarkedForDeletion())
                    .thenThrow(new RuntimeException("DB unavailable"));

            // when & then
            assertThatCode(() -> cleaner.clean()).doesNotThrowAnyException();
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
