package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.dto.StorageItem;
import com.waynehays.cloudfilestorage.service.resource.downloader.ResourceDownloader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDownloaderTest {
    private static final Long USER_ID = 1L;

    @Mock
    private ArchiverApi archiver;

    @Mock
    private FileStorageApi fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @InjectMocks
    private ResourceDownloader resourceDownloader;

    @Nested
    class DownloadFile {

        @Test
        void shouldReturnDownloadResultForFile() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            MetaData metaData = new MetaData(objectKey, "file.txt", 100L, "text/plain", false);
            StorageItem storageItem = new StorageItem(metaData, new ByteArrayInputStream(new byte[0]));

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getObject(objectKey)).thenReturn(Optional.of(storageItem));

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        void shouldStreamFileContent() throws IOException {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            byte[] content = "file content".getBytes();
            MetaData metaData = new MetaData(objectKey, "file.txt", (long) content.length, "text/plain", false);
            StorageItem storageItem = new StorageItem(metaData, new ByteArrayInputStream(content));

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getObject(objectKey)).thenReturn(Optional.of(storageItem));

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            result.body().writeTo(outputStream);

            // then
            assertThat(outputStream.toByteArray()).isEqualTo(content);
        }

        @Test
        void shouldThrowWhenFileNotFound() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> resourceDownloader.download(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);
        }
    }

    @Nested
    class DownloadDirectory {

        @Test
        void shouldReturnDownloadResultForDirectory() {
            // given
            String path = "directory/subdirectory/";
            String objectKey = "user-1-files/directory/subdirectory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getListRecursive(objectKey)).thenReturn(List.of());
            when(archiver.getExtension()).thenReturn(".zip");

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("subdirectory.zip");
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        void shouldCollectArchiveItemsFromDirectoryContents() {
            // given
            String path = "directory/";
            String objectKey = "user-1-files/directory/";
            String childKey = "user-1-files/directory/file.txt";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getListRecursive(objectKey)).thenReturn(List.of(
                    new MetaData(childKey, "file.txt", 100L, "text/plain", true)
            ));
            when(keyResolver.extractPath(USER_ID, childKey)).thenReturn("directory/file.txt");
            when(archiver.getExtension()).thenReturn(".zip");

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("directory.zip");
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        void shouldFilterOutDirectoryMarkersFromArchive() {
            // given
            String path = "directory/";
            String objectKey = "user-1-files/directory/";
            String childFile = "user-1-files/directory/file.txt";
            String childDir = "user-1-files/directory/subdirectory/";

            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(fileStorage.exists(objectKey)).thenReturn(true);
            when(fileStorage.getListRecursive(objectKey)).thenReturn(List.of(
                    new MetaData(childFile, "file.txt", 100L, "text/plain", true),
                    new MetaData(childDir, "subdirectory/", 0L, "text/plain", true)
            ));
            when(keyResolver.extractPath(USER_ID, childFile)).thenReturn("directory/file.txt");
            when(archiver.getExtension()).thenReturn(".zip");

            // when
            resourceDownloader.download(USER_ID, path);

            // then
            verify(keyResolver, never()).extractPath(USER_ID, childDir);
        }
    }
}
