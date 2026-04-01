import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.deleter.ResourceDeleter;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDeleterTest {

    @Mock
    private ResourceStorageApi resourceStorage;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceStorageKeyResolverApi keyResolver;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceDeleter resourceDeleter;

    private static final Long USER_ID = 1L;

    @Nested
    class DeleteFile {

        @Test
        void shouldDeleteFileInCorrectOrder() {
            // given
            String path = "docs/report.pdf";
            String objectKey = "user-1/docs/report.pdf";
            long fileSize = 2048L;

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenReturn(new ResourceMetadataDto(path, fileSize));
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            InOrder inOrder = inOrder(metadataService, resourceStorage, quotaService);
            inOrder.verify(metadataService).markForDeletion(USER_ID, path);
            inOrder.verify(resourceStorage).deleteObject(objectKey);
            inOrder.verify(metadataService).delete(USER_ID, path);
            inOrder.verify(quotaService).releaseSpace(USER_ID, fileSize);
        }

        @Test
        void shouldReleaseSpaceEvenWhenSizeIsZero() {
            // given
            String path = "empty-file.txt";
            String objectKey = "user-1/empty-file.txt";

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenReturn(new ResourceMetadataDto(path, 0L));
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            verify(quotaService).releaseSpace(USER_ID, 0L);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        void shouldDeleteDirectoryInCorrectOrder() {
            // given
            String path = "docs/archive/";
            String objectKey = "user-1/docs/archive/";
            long totalSize = 10240L;

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenReturn(new ResourceMetadataDto(path, 0L));
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(metadataService.sumResourceSizesByPrefix(USER_ID, path)).thenReturn(totalSize);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            InOrder inOrder = inOrder(metadataService, resourceStorage, quotaService);
            inOrder.verify(metadataService).sumResourceSizesByPrefix(USER_ID, path);
            inOrder.verify(metadataService).markForDeletionByPrefix(USER_ID, path);
            inOrder.verify(resourceStorage).deleteByPrefix(objectKey);
            inOrder.verify(metadataService).deleteByPrefix(USER_ID, path);
            inOrder.verify(quotaService).releaseSpace(USER_ID, totalSize);
        }

        @Test
        void shouldNotReleaseSpaceWhenDirectoryTotalSizeIsZero() {
            // given
            String path = "empty-dir/";
            String objectKey = "user-1/empty-dir/";

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenReturn(new ResourceMetadataDto(path, 0L));
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(metadataService.sumResourceSizesByPrefix(USER_ID, path)).thenReturn(0L);

            // when
            resourceDeleter.delete(USER_ID, path);

            // then
            verify(quotaService, never()).releaseSpace(any(), anyLong());
        }
    }

    @Nested
    class DeleteRouting {

        @Test
        void shouldRouteToFileDeleteWhenPathIsFile() {
            // given
            String filePath = "photo.jpg";

            when(metadataService.findOrThrow(USER_ID, filePath))
                    .thenReturn(new ResourceMetadataDto(filePath, 500L));
            when(keyResolver.resolveKey(USER_ID, filePath)).thenReturn("user-1/photo.jpg");

            // when
            resourceDeleter.delete(USER_ID, filePath);

            // then
            verify(metadataService).markForDeletion(USER_ID, filePath);
            verify(resourceStorage).deleteObject(any());
            verify(metadataService, never()).markForDeletionByPrefix(any(), any());
            verify(resourceStorage, never()).deleteByPrefix(any());
        }

        @Test
        void shouldRouteToDirectoryDeleteWhenPathIsDirectory() {
            // given
            String dirPath = "folder/";

            when(metadataService.findOrThrow(USER_ID, dirPath))
                    .thenReturn(new ResourceMetadataDto(dirPath, 0L));
            when(keyResolver.resolveKey(USER_ID, dirPath)).thenReturn("user-1/folder/");
            when(metadataService.sumResourceSizesByPrefix(USER_ID, dirPath)).thenReturn(0L);

            // when
            resourceDeleter.delete(USER_ID, dirPath);

            // then
            verify(metadataService).markForDeletionByPrefix(USER_ID, dirPath);
            verify(resourceStorage).deleteByPrefix(any());
            verify(metadataService, never()).markForDeletion(any(), any());
            verify(resourceStorage, never()).deleteObject(any());
        }
    }

    @Nested
    class DeletePropagation {

        @Test
        void shouldPropagateExceptionFromFindOrThrow() {
            // given
            String path = "nonexistent.txt";

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenThrow(new RuntimeException("Resource not found"));

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> resourceDeleter.delete(USER_ID, path)
            );

            verifyNoInteractions(keyResolver, resourceStorage, quotaService);
        }
    }
}