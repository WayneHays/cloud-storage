package com.waynehays.cloudfilestorage.resource.archive;

import com.waynehays.cloudfilestorage.shared.exception.ArchiveException;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
class ZipArchiverTest {

    private final ZipArchiver zipArchiver = new ZipArchiver();

    @Nested
    class ArchiveFiles {

        @Test
        @DisplayName("Should create archive with single file")
        void shouldCreateArchiveWithSingleFile() throws IOException {
            // given
            byte[] content = "text".getBytes();
            ArchiveItem item = new ArchiveItem("file.txt", content.length,
                    () -> new ByteArrayInputStream(content));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveResources(List.of(item), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).containsEntry("file.txt", content);
        }

        @Test
        @DisplayName("Should create archive with multiple files")
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
            zipArchiver.archiveResources(List.of(item1, item2), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).hasSize(2)
                    .containsEntry("file1.txt", content1)
                    .containsEntry("file2.txt", content2);
        }

        @Test
        @DisplayName("Should preserve directory structure in entry names")
        void shouldPreserveDirectoryStructureInEntryNames() throws IOException {
            // given
            byte[] content = "nested file".getBytes();
            ArchiveItem item = new ArchiveItem("folder/subfolder/file.txt", content.length,
                    () -> new ByteArrayInputStream(content));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveResources(List.of(item), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).containsKey("folder/subfolder/file.txt");
        }

        @Test
        @DisplayName("Should create empty archive")
        void shouldCreateEmptyArchive() throws IOException {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when
            zipArchiver.archiveResources(List.of(), outputStream);

            // then
            Map<String, byte[]> entries = extractZipEntries(outputStream.toByteArray());
            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("Should throw ArchiveException when IO error")
        void shouldThrowArchiveExceptionOnIOError() {
            // given
            ArchiveItem item = new ArchiveItem("file.txt", 5L,
                    () -> {
                        throw new IOException("stream error");
                    });
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // when & then
            List<ArchiveItem> items = List.of(item);
            assertThatThrownBy(() -> zipArchiver.archiveResources(items, outputStream))
                    .isInstanceOf(ArchiveException.class);
        }
    }

    @Test
    @DisplayName("getExtension should return ZIP extension")
    void getExtension_shouldReturnZipExtension() {
        // given & when & then
        assertThat(zipArchiver.getExtension()).isEqualTo(".zip");
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
