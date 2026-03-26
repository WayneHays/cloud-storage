package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.deleter.ResourceDeleter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDeleterTest {

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @InjectMocks
    private ResourceDeleter resourceDeleter;

    private static final Long USER_ID = 1L;

    @Nested
    class DeleteFile {

        @Test
        void shouldDeleteFile() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(metadata);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            InOrder inOrder = inOrder(metadataService, storage);
            inOrder.verify(metadataService).markForDeletion(USER_ID, path);
            inOrder.verify(storage).deleteObject(objectKey);
            inOrder.verify(metadataService).delete(USER_ID, path);
        }

        @Test
        void shouldThrowWhenFileNotFound() {
            // given
            String path = "directory/file.txt";

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenThrow(new ResourceNotFoundException("Resource not found", path));

            // when & then
            assertThatThrownBy(() -> resourceDeleter.delete(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(storage, never()).deleteObject(any());
            verify(metadataService, never()).markForDeletion(any(), any());
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        void shouldDeleteDirectory() {
            // given
            String path = "directory/subdirectory/";
            String objectKey = "user-1-files/directory/subdirectory/";
            ResourceMetadata metadata = new ResourceMetadata();

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(metadata);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            InOrder inOrder = inOrder(metadataService, storage);
            inOrder.verify(metadataService).markForDeletionByPrefix(USER_ID, path);
            inOrder.verify(storage).deleteByPrefix(objectKey);
            inOrder.verify(metadataService).deleteByPrefix(USER_ID, path);
        }
    }
}
