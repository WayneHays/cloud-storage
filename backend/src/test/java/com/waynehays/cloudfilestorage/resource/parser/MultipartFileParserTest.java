package com.waynehays.cloudfilestorage.resource.parser;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.shared.exception.MultipartValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MultipartFileParserTest {

    private MultipartFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new MultipartFileParser();
    }

    private MultipartFile mockFile(String originalFilename, long size, String contentType) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(originalFilename);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.getContentType()).thenReturn(contentType);
        return file;
    }

    @Nested
    class ParseAll {

        @Test
        void shouldParseAllFiles() {
            // given
            MultipartFile file1 = mockFile("a.txt", 100L, "text/plain");
            MultipartFile file2 = mockFile("b.txt", 200L, "text/plain");

            // when
            List<UploadObjectDto> result = parser.parseAll(List.of(file1, file2), "docs/");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(UploadObjectDto::fullPath)
                    .containsExactlyInAnyOrder("docs/a.txt", "docs/b.txt");
        }

        @Test
        void shouldReturnEmptyList_whenNoFilesProvided() {
            // given & when
            List<UploadObjectDto> result = parser.parseAll(List.of(), "docs/");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrow_whenAnyFileHasBlankFilename() {
            // given
            MultipartFile file = mockFile("", 100L, "text/plain");

            // when & then
            assertThatThrownBy(() -> parser.parseAll(List.of(file), "docs/"))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrow_whenAnyFileHasNullFilename() {
            // given
            MultipartFile file = mockFile(null, 100L, "text/plain");

            // when & then
            assertThatThrownBy(() -> parser.parseAll(List.of(file), "docs/"))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class ParseSingleFile {

        @Test
        void shouldCorrectlyPopulateAllFields() {
            // given
            MultipartFile file = mockFile("file.txt", 512L, "text/plain");

            // when
            List<UploadObjectDto> result = parser.parseAll(List.of(file), "docs/");
            UploadObjectDto dto = result.getFirst();

            // then
            assertThat(dto.originalFilename()).isEqualTo("file.txt");
            assertThat(dto.filename()).isEqualTo("file.txt");
            assertThat(dto.directory()).isEqualTo("docs");
            assertThat(dto.fullPath()).isEqualTo("docs/file.txt");
            assertThat(dto.size()).isEqualTo(512L);
            assertThat(dto.contentType()).isEqualTo("text/plain");
        }

        @Test
        void shouldUseDefaultContentType_whenContentTypeIsNull() {
            // given
            MultipartFile file = mockFile("file.txt", 100L, null);

            // when
            UploadObjectDto dto = parser.parseAll(List.of(file), "docs/").getFirst();

            // then
            assertThat(dto.contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        void shouldHandleNestedPath_whenFilenameContainsSubdirectory() {
            // given
            MultipartFile file = mockFile("sub/nested/file.txt", 100L, "text/plain");

            // when
            UploadObjectDto dto = parser.parseAll(List.of(file), "docs/").getFirst();

            // then
            assertThat(dto.filename()).isEqualTo("file.txt");
            assertThat(dto.directory()).isEqualTo("docs/sub/nested");
            assertThat(dto.fullPath()).isEqualTo("docs/sub/nested/file.txt");
        }

        @Test
        void shouldNormalizeBackslashes_whenWindowsPathProvided() {
            // given
            MultipartFile file = mockFile("sub\\file.txt", 100L, "text/plain");

            // when
            UploadObjectDto dto = parser.parseAll(List.of(file), "docs/").getFirst();

            // then
            assertThat(dto.fullPath()).isEqualTo("docs/sub/file.txt");
        }

        @Test
        void shouldUploadToRoot_whenDirectoryIsEmpty() {
            // given
            MultipartFile file = mockFile("file.txt", 100L, "text/plain");

            // when
            UploadObjectDto dto = parser.parseAll(List.of(file), "").getFirst();

            // then
            assertThat(dto.directory()).isEqualTo("");
            assertThat(dto.fullPath()).isEqualTo("file.txt");
        }
    }
}

