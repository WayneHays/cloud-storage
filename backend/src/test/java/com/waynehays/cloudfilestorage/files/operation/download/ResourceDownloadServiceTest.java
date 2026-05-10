package com.waynehays.cloudfilestorage.files.operation.download;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.operation.download.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.files.operation.download.dto.DownloadResult;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.dto.StorageItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDownloadServiceTest {

    @Mock
    private ArchiverApi archiver;

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceDownloadService service;

    private static final Long USER_ID = 1L;

    @Nested
    class DownloadFile {

        @Test
        @DisplayName("Should return File result with correct name and content type")
        void shouldReturnFileResult() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "docs/report.pdf", "docs/", "report.pdf",
                    2048L, ResourceType.FILE);

            when(metadataService.findByPath(USER_ID, "docs/report.pdf")).thenReturn(file);

            // when
            DownloadResult result = service.download(USER_ID, "docs/report.pdf");

            // then
            assertThat(result).isInstanceOf(DownloadResult.File.class);
            assertThat(result.name()).isEqualTo("report.pdf");
            assertThat(result.contentType()).isEqualTo("application/octet-stream");

            DownloadResult.File fileResult = (DownloadResult.File) result;
            assertThat(fileResult.contentSupplier()).isNotNull();

            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("Should open storage stream only when supplier is invoked")
        void shouldOpenStreamLazily() throws IOException {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "docs/report.pdf", "docs/", "report.pdf",
                    2048L, ResourceType.FILE);
            StorageItem item = new StorageItem(new ByteArrayInputStream("content".getBytes()));

            when(metadataService.findByPath(USER_ID, "docs/report.pdf")).thenReturn(file);
            when(storageService.getObject(USER_ID, "storage-key")).thenReturn(Optional.of(item));

            // when
            DownloadResult result = service.download(USER_ID, "docs/report.pdf");
            verifyNoInteractions(storageService);

            InputStream stream = ((DownloadResult.File) result).contentSupplier().getInputStream();

            // then
            verify(storageService).getObject(USER_ID, "storage-key");
            assertThat(stream).isNotNull();
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException from supplier when storage is inconsistent")
        void shouldPropagateNotFoundFromSupplier() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "docs/report.pdf", "docs/", "report.pdf",
                    2048L, ResourceType.FILE);

            when(metadataService.findByPath(USER_ID, "docs/report.pdf")).thenReturn(file);
            when(storageService.getObject(USER_ID, "storage-key"))
                    .thenThrow(new ResourceNotFoundException("Not found in storage", "docs/report.pdf"));

            DownloadResult result = service.download(USER_ID, "docs/report.pdf");
            DownloadResult.File fileResult = (DownloadResult.File) result;

            // when & then
            assertThatThrownBy(() -> fileResult.contentSupplier().getInputStream())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DownloadDirectory {

        @Test
        @DisplayName("Should return Archive result with correct name and content type")
        void shouldReturnArchiveResult() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, null, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file = new ResourceMetadataDto(
                    3L, USER_ID, "storage-key", "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findByPath(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of(file));
            when(archiver.getExtension()).thenReturn(".zip");
            when(archiver.getContentType()).thenReturn("application/zip");

            // when
            DownloadResult result = service.download(USER_ID, "docs/");

            // then
            assertThat(result).isInstanceOf(DownloadResult.Archive.class);
            assertThat(result.name()).isEqualTo("docs.zip");
            assertThat(result.contentType()).isEqualTo("application/zip");
            assertThat(((DownloadResult.Archive) result).writer()).isNotNull();
        }

        @Test
        @DisplayName("Should invoke archiver when writer is called")
        void shouldInvokeArchiverOnWrite() throws IOException {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, null, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file = new ResourceMetadataDto(
                    3L, USER_ID, "storage-key", "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findByPath(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of(file));
            when(archiver.getExtension()).thenReturn(".zip");
            when(archiver.getContentType()).thenReturn("application/zip");

            DownloadResult result = service.download(USER_ID, "docs/");
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            verify(archiver, never()).archiveResources(anyList(), any());

            // when
            ((DownloadResult.Archive) result).writer().writeTo(out);

            // then
            verify(archiver).archiveResources(anyList(), eq(out));
        }
    }

    @Test
    @DisplayName("Should throw exception when metadata not found in database")
    void shouldThrowWhenResourceNotFound() {
        // given
        when(metadataService.findByPath(USER_ID, "missing.txt"))
                .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

        // when & then
        assertThatThrownBy(() -> service.download(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(storageService);
    }
}
