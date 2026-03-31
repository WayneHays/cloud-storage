package com.waynehays.cloudfilestorage.unit.component.parser;

import com.waynehays.cloudfilestorage.component.MultipartFileDataParser;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.component.validator.MultipartFileValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MultipartFileDataParserTest {

    @Mock
    private MultipartFileValidator validator;

    @InjectMocks
    private MultipartFileDataParser parser;

    @Nested
    class Parse {

        @Test
        void shouldParseBasicFile() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.txt", "text/plain", "content".getBytes());

            // when
            ObjectData result = parser.parse(file, "directory/");

            // then
            assertThat(result.originalFilename()).isEqualTo("document.txt");
            assertThat(result.filename()).isEqualTo("document.txt");
            assertThat(result.directory()).isEqualTo("directory");
            assertThat(result.fullPath()).isEqualTo("directory/document.txt");
            assertThat(result.size()).isEqualTo(7L);
            assertThat(result.contentType()).isEqualTo("text/plain");
        }

        @Test
        void shouldParseFileWithNestedPath() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "subdirectory/document.txt", "text/plain", "content".getBytes());

            // when
            ObjectData result = parser.parse(file, "directory/");

            // then
            assertThat(result.filename()).isEqualTo("document.txt");
            assertThat(result.directory()).isEqualTo("directory/subdirectory");
            assertThat(result.fullPath()).isEqualTo("directory/subdirectory/document.txt");
        }

        @Test
        void shouldNormalizeBackslashes() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "sub\\directory\\file.txt", "text/plain", "content".getBytes());

            // when
            ObjectData result = parser.parse(file, "root/");

            // then
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.directory()).isEqualTo("root/sub/directory");
        }

        @Test
        void shouldPreserveNullContentType() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "file.bin", null, "content".getBytes());

            // when
            ObjectData result = parser.parse(file, "directory/");

            // then
            assertThat(result.contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        void shouldProvideWorkingInputStreamSupplier() throws IOException {
            // given
            byte[] content = "file content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "file.txt", "text/plain", content);

            // when
            ObjectData result = parser.parse(file, "directory/");

            // then
            try (InputStream is = result.inputStreamSupplier().get()) {
                assertThat(is.readAllBytes()).isEqualTo(content);
            }
        }

        @Test
        void shouldParseFileInRootDirectory() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "file.txt", "text/plain", "content".getBytes());

            // when
            ObjectData result = parser.parse(file, "");

            // then
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.fullPath()).isEqualTo("file.txt");
        }
    }
}
