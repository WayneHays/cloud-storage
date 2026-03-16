package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.service.resource.deleter.ResourceDeleter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDeleterTest {
    private static final Long USER_ID = 1L;

    @Mock
    private FileStorageApi fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;


    @InjectMocks
    private ResourceDeleter resourceDeleter;

    @Nested
    class DeleteFile {

        @Test
        void shouldDeleteSingleFile() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            verify(fileStorage).delete(objectKey);
            verify(fileStorage, never()).deleteObjects(any());
        }

        @Test
        void shouldThrowWhenFileNotFound() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> resourceDeleter.delete(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);

            verify(fileStorage, never()).delete(any());
            verify(fileStorage, never()).deleteObjects(any());
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        void shouldDeleteDirectoryWithContents() {
            // given
            String path = "directory/subdirectory/";
            String objectKey = "user-1-files/directory/subdirectory/";
            String childKey1 = objectKey + "file1.txt";
            String childKey2 = objectKey + "file2.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getListRecursive(objectKey)).thenReturn(List.of(
                    new MetaData(childKey1, "file1.txt", 100L, "text/plain", false),
                    new MetaData(childKey2, "file2.txt", 200L, "text/plain", false)
            ));

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            verify(fileStorage).deleteObjects(List.of(childKey1, childKey2, objectKey));
        }

        @Test
        void shouldDeleteEmptyDirectory() {
            // given
            String path = "folder/subfolder/";
            String objectKey = "user-1-files/folder/subfolder/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getListRecursive(objectKey)).thenReturn(List.of());

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            verify(fileStorage).deleteObjects(List.of(objectKey));
        }
    }
}
