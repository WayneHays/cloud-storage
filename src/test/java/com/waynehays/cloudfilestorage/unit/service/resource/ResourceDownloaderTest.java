package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolver;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
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

    @Mock
    private ArchiverApi archiver;

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private ResourceStorageKeyResolver keyResolver;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceDownloader resourceDownloader;

    private static final Long USER_ID = 1L;

    @Nested
    class DownloadFile {

        @Test
        void shouldReturnDownloadResultForFile() {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            ResourceMetadata metadata = new ResourceMetadata();
            StorageItem storageItem = new StorageItem(new ByteArrayInputStream(new byte[0]));

            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(metadata);
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(storage.getObject(objectKey)).thenReturn(Optional.of(storageItem));

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        void shouldStreamFileContent() throws IOException {
            // given
            String path = "directory/file.txt";
            String objectKey = "user-1-files/directory/file.txt";
            byte[] content = "file content".getBytes();
            ResourceMetadata metadata = new ResourceMetadata();
            StorageItem storageItem = new StorageItem(new ByteArrayInputStream(content));

            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(metadata);
            when(keyResolver.resolveKey(USER_ID, path)).thenReturn(objectKey);
            when(storage.getObject(objectKey)).thenReturn(Optional.of(storageItem));

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

            when(metadataService.findOrThrow(USER_ID, path))
                    .thenThrow(new ResourceNotFoundException("Resource not found", path));

            // when & then
            assertThatThrownBy(() -> resourceDownloader.download(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DownloadDirectory {

        @Test
        void shouldReturnDownloadResultForDirectory() {
            // given
            String path = "directory/subdirectory/";
            ResourceMetadata metadata = new ResourceMetadata();

            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(metadata);
            when(metadataService.findDirectoryContent(USER_ID, path)).thenReturn(List.of());
            when(archiver.getExtension()).thenReturn(".zip");
            when(archiver.getContentType()).thenReturn("application/zip");

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("subdirectory.zip");
            assertThat(result.contentType()).isEqualTo("application/zip");
        }

        @Test
        void shouldCollectArchiveItemsFromDirectoryContents() {
            // given
            String path = "directory/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata fileMetadata = createFileMetadata();

            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(dirMetadata);
            when(metadataService.findDirectoryContent(USER_ID, path)).thenReturn(List.of(fileMetadata));
            when(keyResolver.resolveKey(USER_ID, "directory/file.txt"))
                    .thenReturn("user-1-files/directory/file.txt");
            when(archiver.getExtension()).thenReturn(".zip");
            when(archiver.getContentType()).thenReturn("application/zip");

            // when
            DownloadResult result = resourceDownloader.download(USER_ID, path);

            // then
            assertThat(result.name()).isEqualTo("directory.zip");
            assertThat(result.contentType()).isEqualTo("application/zip");
        }

        @Test
        void shouldFilterOutDirectoriesFromArchive() {
            // given
            String path = "directory/";
            ResourceMetadata dirMetadata = new ResourceMetadata();
            ResourceMetadata fileMetadata = createFileMetadata();
            ResourceMetadata subDirMetadata = createDirectoryMetadata();

            when(metadataService.findOrThrow(USER_ID, path)).thenReturn(dirMetadata);
            when(metadataService.findDirectoryContent(USER_ID, path))
                    .thenReturn(List.of(fileMetadata, subDirMetadata));
            when(keyResolver.resolveKey(USER_ID, "directory/file.txt"))
                    .thenReturn("user-1-files/directory/file.txt");
            when(archiver.getExtension()).thenReturn(".zip");

            // when
            resourceDownloader.download(USER_ID, path);

            // then
            verify(keyResolver, never()).resolveKey(USER_ID, "directory/subdirectory/");
        }
    }

    private ResourceMetadata createFileMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/file.txt");
        metadata.setSize(100L);
        metadata.setType(ResourceType.FILE);
        return metadata;
    }

    private ResourceMetadata createDirectoryMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setPath("directory/subdirectory/");
        metadata.setType(ResourceType.DIRECTORY);
        return metadata;
    }
}
