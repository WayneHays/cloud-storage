package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.dto.internal.StorageItem;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.DownloadResult;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.download.ResourceDownloadService;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDownloadServiceTest {

    @Mock
    private ArchiverApi archiver;

    @Mock
    private ResourceStorageService storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceDownloadService service;

    private static final Long USER_ID = 1L;

    @Nested
    class DownloadFile {

        @Test
        void shouldReturnDownloadResultWithFilename() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "docs/report.pdf", "docs/", "report.pdf",
                    2048L, ResourceType.FILE);
            StorageItem item = new StorageItem(new ByteArrayInputStream(new byte[0]));

            when(metadataService.findOrThrow(USER_ID, "docs/report.pdf")).thenReturn(file);
            when(storageService.getObject(USER_ID, "docs/report.pdf")).thenReturn(item);

            // when
            DownloadResult result = service.download(USER_ID, "docs/report.pdf");

            // then
            assertThat(result.name()).isEqualTo("report.pdf");
            assertThat(result.contentType()).isEqualTo("application/octet-stream");
            assertThat(result.body()).isNotNull();
        }
    }

    @Nested
    class DownloadDirectory {

        @Test
        void shouldReturnDownloadResultWithArchiveFilename() {
            // given
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);
            ResourceMetadataDto file = new ResourceMetadataDto(
                    3L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(metadataService.findOrThrow(USER_ID, "docs/")).thenReturn(dir);
            when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(List.of(file));
            when(archiver.getExtension()).thenReturn(".zip");
            when(archiver.getContentType()).thenReturn("application/zip");

            // when
            DownloadResult result = service.download(USER_ID, "docs/");

            // then
            assertThat(result.name()).isEqualTo("docs.zip");
            assertThat(result.contentType()).isEqualTo("application/zip");
            assertThat(result.body()).isNotNull();
        }
    }

    @Test
    void shouldThrowWhenResourceNotFound() {
        // given
        when(metadataService.findOrThrow(USER_ID, "missing.txt"))
                .thenThrow(new ResourceNotFoundException("Resource not found", "missing.txt"));

        // when & then
        assertThatThrownBy(() -> service.download(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(storageService);
    }
}
