package com.waynehays.cloudfilestorage.unit.component.archiver;

import com.waynehays.cloudfilestorage.component.archiver.ZipArchiver;
import com.waynehays.cloudfilestorage.config.properties.ArchiveProperties;
import com.waynehays.cloudfilestorage.component.archiver.ArchiveItem;
import com.waynehays.cloudfilestorage.exception.ArchiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ZipArchiverTest {

    private ZipArchiver zipArchiver;

    @BeforeEach
    void setUp() {
        ArchiveProperties archiveProperties = mock(ArchiveProperties.class);
        lenient().when(archiveProperties.bufferSize()).thenReturn(8192);
        zipArchiver = new ZipArchiver(archiveProperties);
    }

    @Nested
    class ArchiveFiles {

        @Test
        void shouldCreateArchiveWithSingleFile() throws IOException {
            // given
            byte[] content = "text".getBytes();
            ArchiveItem item = new ArchiveItem("file.txt", content.length,
                    () -> new ByteArrayInputStream(content));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveFiles(List.of(item), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).containsEntry("file.txt", content);
        }

        @Test
        void shouldCreateArchiveWithMultipleFiles() throws IOException {
            // given
            byte[] content1 = "first file".getBytes();
            byte[] content2 = "second file".getBytes();
            ArchiveItem item1 = new ArchiveItem("file1.txt", content1.length,
                    () -> new ByteArrayInputStream(content1));
            ArchiveItem item2 = new ArchiveItem("file2.txt", content2.length,
                    () -> new ByteArrayInputStream(content2));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveFiles(List.of(item1, item2), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).hasSize(2)
                    .containsEntry("file1.txt", content1)
                    .containsEntry("file2.txt", content2);
        }

        @Test
        void shouldPreserveDirectoryStructureInEntryNames() throws IOException {
            // given
            byte[] content = "nested file".getBytes();
            ArchiveItem item = new ArchiveItem("folder/subfolder/file.txt", content.length,
                    () -> new ByteArrayInputStream(content));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveFiles(List.of(item), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).containsKey("folder/subfolder/file.txt");
        }

        @Test
        void shouldCreateEmptyArchive() throws IOException {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveFiles(List.of(), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).isEmpty();
        }

        @Test
        void shouldThrowArchiveExceptionOnIOError() {
            // given
            ArchiveItem item = new ArchiveItem("file.txt", 5L,
                    () -> {
                        throw new IOException("stream error");
                    });
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when & then
            List<ArchiveItem> items = List.of(item);
            assertThatThrownBy(() -> zipArchiver.archiveFiles(items, outputStream))
                    .isInstanceOf(ArchiveException.class);
        }
    }

    @Nested
    class GetExtension {

        @Test
        void shouldReturnZipExtension() {
            // given & when & then
            assertThat(zipArchiver.getExtension()).isEqualTo(".zip");
        }
    }

    private Map<String, byte[]> extractZipEntries(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
            }
        }
        return entries;
    }
}
