package com.waynehays.cloudfilestorage.unit.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.parser.MultipartFileParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MultipartFileParserTest {

    private final MultipartFileParser parser = new MultipartFileParser();

    @Test
    @DisplayName("Should parse simple file")
    void shouldParseSimpleFile() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "content".getBytes());

        // when
        UploadObjectDto result = parser.parse(file, "directory/");

        // then
        assertThat(result.originalFilename()).isEqualTo("document.txt");
        assertThat(result.filename()).isEqualTo("document.txt");
        assertThat(result.directory()).isEqualTo("directory");
        assertThat(result.fullPath()).isEqualTo("directory/document.txt");
        assertThat(result.size()).isEqualTo(7L);
        assertThat(result.contentType()).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("Should parse file with nested path in originalFilename")
    void shouldParseFileWithNestedPath() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "subdirectory/document.txt", "text/plain", "content".getBytes());

        // when
        UploadObjectDto result = parser.parse(file, "directory/");

        // then
        assertThat(result.filename()).isEqualTo("document.txt");
        assertThat(result.directory()).isEqualTo("directory/subdirectory");
        assertThat(result.fullPath()).isEqualTo("directory/subdirectory/document.txt");
    }

    @Test
    @DisplayName("Should normalize back slashes")
    void shouldNormalizeBackslashes() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "sub\\directory\\file.txt", "text/plain", "content".getBytes());

        // when
        UploadObjectDto result = parser.parse(file, "root/");

        // then
        assertThat(result.filename()).isEqualTo("file.txt");
        assertThat(result.directory()).isEqualTo("root/sub/directory");
    }

    @Test
    @DisplayName("Should preserve null content type")
    void shouldPreserveNullContentType() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.bin", null, "content".getBytes());

        // when
        UploadObjectDto result = parser.parse(file, "directory/");

        // then
        assertThat(result.contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("Should provide working with InputStreamSupplier")
    void shouldProvideWorkingInputStreamSupplier() throws IOException {
        // given
        byte[] content = "file content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.txt", "text/plain", content);

        // when
        UploadObjectDto result = parser.parse(file, "directory/");

        // then
        try (InputStream is = result.inputStreamSupplier().get()) {
            assertThat(is.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("Should parse file in root directory")
    void shouldParseFileInRootDirectory() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.txt", "text/plain", "content".getBytes());

        // when
        UploadObjectDto result = parser.parse(file, "");

        // then
        assertThat(result.filename()).isEqualTo("file.txt");
        assertThat(result.fullPath()).isEqualTo("file.txt");
    }
}

