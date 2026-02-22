package com.waynehays.cloudfilestorage.unit.extractor;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.extractor.MultipartFileDataExtractorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartFileDataExtractorImplTest {

    private final MultipartFileDataExtractorImpl multipartFileDataExtractor = new MultipartFileDataExtractorImpl();
    private MultipartFile file;

    @Nested
    @DisplayName("Extraction method tests")
    class ExtractMethodTests {

        @Test
        @DisplayName("Should extract data from simple file")
        void shouldExtractDataFromSimpleFile() throws IOException {
            // given
            file = createMock("file.txt", 10L, "text");
            String directory = "documents";

            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.originalFilename()).isEqualTo("file.txt");
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.directory()).isEqualTo(directory);
            assertThat(result.size()).isEqualTo(10L);
            assertThat(result.contentType()).isEqualTo("text");
        }

        @Test
        @DisplayName("Should extract data from file with embedded folders")
        void shouldExtractDataFromFileWithEmbeddedFolders() throws IOException {
            // given
            file = createMock("folder1/folder2/file.txt", 10L, "text");
            String directory = "documents";

            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.directory()).isEqualTo("documents/folder1/folder2");
        }

        @Test
        @DisplayName("Should extract data from file with multiple extensions")
        void shouldExtractDataFromFileWithMultipleExtensions() throws IOException {
            // given
            file = createMock("archive.tar.gz", 10L, "text");

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.originalFilename()).isEqualTo("archive.tar.gz");
            assertThat(result.directory()).isEmpty();
            assertThat(result.extension()).isEqualTo("gz");
        }

        @Test
        @DisplayName("Should extract data from file without extension")
        void shouldExtractDataFromFileWithoutExtension() throws IOException {
            // given
            file = createMock("README", 10L, "text");

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.extension()).isEmpty();
        }

        @Test
        @DisplayName("Should use default content type when null")
        void shouldUseDefaultContentTypeWhenNull() throws IOException {
            // given
            file = createMock("test", 10L, null);
            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should preserve provided content type")
        void shouldPreserveProvidedContentType() throws IOException {
            // given
            file = createMock("test", 10L, "test");

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.contentType()).isEqualTo("test");
        }

        @Test
        @DisplayName("Should throw FileStorageException when getInputStream fails")
        void shouldThrowFileStorageExceptionWhenGetInputStreamFails() throws IOException {
            // given
            file = createMock(null, 1, null);
            when(file.getInputStream()).thenThrow(new FileStorageException(null, null));

            // when & then
            assertThrows(FileStorageException.class, () -> multipartFileDataExtractor.extract(file, null));
        }
    }

    @Nested
    @DisplayName("Directory merge tests")
    class DirectoryMergeTests {

        @Test
        @DisplayName("Should merge base and sub directories")
        void shouldMergeBaseAndSubDirectories() throws IOException {
            // given
            file = createMock("work/projects/file.txt", 1, null);
            String directory = "documents";

            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.directory()).isEqualTo("documents/work/projects");
        }

        @Test
        @DisplayName("Should use sub directory when base is empty")
        void shouldUseSubDirectoryWhenBaseIsEmpty() throws IOException {
            // given
            file = createMock("folder/file.txt", 1, null);
            String directory = "";

            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.directory()).isEqualTo("folder");
        }

        @Test
        @DisplayName("Should use sub directory when base is null")
        void shouldUseSubDirectoryWhenBaseIsNull() throws IOException {
            // given
            file = createMock("folder/file.txt", 1, null);

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.directory()).isEqualTo("folder");
        }

        @Test
        @DisplayName("Should use base directory when sub is empty")
        void shouldUseBaseDirectoryWhenSubIsEmpty() throws IOException {
            // given
            file = createMock("", 1, null);
            String directory = "documents";
            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.directory()).isEqualTo(directory);
        }

        @Test
        @DisplayName("Should return empty when both directories are empty")
        void shouldReturnEmptyWhenBothDirectoriesAreEmpty() throws IOException {
            // given
            file = createMock("", 1, null);
            String directory = "";

            // when
            FileData result = multipartFileDataExtractor.extract(file, directory);

            // then
            assertThat(result.directory()).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when both directories are null")
        void shouldReturnEmptyWhenBothDirectoriesAreNull() throws IOException {
            // given
            file = createMock("", 1, null);

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.directory()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Directory normalization tests")
    class DirectoryNormalizationTests {

        private static Stream<Arguments> provideSuccessNormalization() {
            return Stream.of(
                    Arguments.of("/documents"),
                    Arguments.of("documents/"),
                    Arguments.of("/documents/"),
                    Arguments.of("///documents"),
                    Arguments.of("documents///"),
                    Arguments.of("   documents")
            );
        }

        @ParameterizedTest
        @MethodSource("provideSuccessNormalization")
        void givenFileAndDirectory_whenExtract_thenSuccess(String directory) throws IOException {
            file = createMock(null, 1, null);
            assertThat(multipartFileDataExtractor.extract(file, directory).directory()).isEqualTo("documents");
        }

        @Test
        @DisplayName("Should normalize Windows-style separators in filename")
        void shouldNormalizeWindowsStyleSeparatorsInFilename() throws IOException {
            // given
            file = createMock("folder\\file.txt", 1, null);

            // when
            FileData result = multipartFileDataExtractor.extract(file, null);

            // then
            assertThat(result.originalFilename()).isEqualTo("folder\\file.txt");
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.directory()).isEqualTo("folder");
        }
    }

    private MultipartFile createMock(String originalFilename, long size, String contentType) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(originalFilename);
        when(file.getSize()).thenReturn(size);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        return file;
    }
}
