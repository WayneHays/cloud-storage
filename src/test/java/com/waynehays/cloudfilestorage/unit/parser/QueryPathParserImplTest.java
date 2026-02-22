package com.waynehays.cloudfilestorage.unit.parser;

import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.exception.InvalidPathException;
import com.waynehays.cloudfilestorage.parser.querypathparser.QueryPathParserImpl;
import com.waynehays.cloudfilestorage.validator.PathValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueryPathParserImplTest {

    @Mock
    private PathValidatorImpl pathValidator;

    @InjectMocks
    private QueryPathParserImpl queryPathParser;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(pathValidator).validateDirectoryPath(any());
    }

    @Nested
    @DisplayName("parse - Directory paths")
    class ParseDirectoryPathTests {

        @Test
        @DisplayName("Should parse null path as root directory")
        void shouldParseNullPathAsRootDirectory() {
            // when
            ParsedPath result = queryPathParser.parse(null);

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(result.isDirectory()).isTrue();
            assertThat(result.isFile()).isFalse();

            verify(pathValidator).validateDirectoryPath(null);
        }

        @Test
        @DisplayName("Should parse empty string as root directory")
        void shouldParseEmptyStringAsRootDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(result.isDirectory()).isTrue();

            verify(pathValidator).validateDirectoryPath("");
        }

        @Test
        @DisplayName("Should parse whitespace-only path as root directory")
        void shouldParseWhitespaceOnlyPathAsRootDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("   ");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("Should parse directory path with trailing slash")
        void shouldParseDirectoryPathWithTrailingSlash() {
            // when
            ParsedPath result = queryPathParser.parse("folder1/folder2/");

            // then
            assertThat(result.directory()).isEqualTo("folder1/folder2");
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(result.isDirectory()).isTrue();
        }

        @Test
        @DisplayName("Should parse single directory with trailing slash")
        void shouldParseSingleDirectoryWithTrailingSlash() {
            // when
            ParsedPath result = queryPathParser.parse("documents/");

            // then
            assertThat(result.directory()).isEqualTo("documents");
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("Should parse nested directory with trailing slash")
        void shouldParseNestedDirectoryWithTrailingSlash() {
            // when
            ParsedPath result = queryPathParser.parse("folder1/folder2/folder3/");

            // then
            assertThat(result.directory()).isEqualTo("folder1/folder2/folder3");
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("Should parse root slash as root directory")
        void shouldParseRootSlashAsRootDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("/");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }
    }

    @Nested
    @DisplayName("parse - File paths")
    class ParseFilePathTests {

        @ParameterizedTest
        @DisplayName("Should parse various file paths correctly")
        @CsvSource({
                "'docs/archive.tar.gz',         docs,         archive.tar.gz",
                "'docs/README',                 docs,         README",
                "'a/b/c/d/e/file.txt',          a/b/c/d/e,    file.txt",
                "'folder/my-file_v1.0.txt',     folder,       my-file_v1.0.txt",
                "'documents/report.pdf',        documents,    report.pdf"
        })
        void shouldParseVariousFilePathsCorrectly(String input, String expectedDirectory, String expectedFilename) {
            // when
            ParsedPath result = queryPathParser.parse(input);

            // then
            assertThat(result.directory()).isEqualTo(expectedDirectory);
            assertThat(result.filename()).isEqualTo(expectedFilename);
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
            assertThat(result.isFile()).isTrue();
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        @DisplayName("Should parse file in nested directory")
        void shouldParseFileInNestedDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("folder1/folder2/file.txt");

            // then
            assertThat(result.directory()).isEqualTo("folder1/folder2");
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
            assertThat(result.isFile()).isTrue();
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        @DisplayName("Should parse file in root directory")
        void shouldParseFileInRootDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("file.txt");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isEqualTo("file.txt");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
            assertThat(result.isFile()).isTrue();
        }
    }

    @Nested
    @DisplayName("parse - Normalization")
    class ParseNormalizationTests {

        @ParameterizedTest
        @DisplayName("Should normalize paths correctly")
        @CsvSource({
                "'  folder/file.txt',     folder,                 file.txt",
                "'folder/file.txt  ',     folder,                 file.txt",
                "'  folder/file.txt  ',   folder,                 file.txt",
                "'folder1\\folder2\\file.txt', 'folder1/folder2', file.txt",
                "'folder1/folder2\\folder3\\file.txt', 'folder1/folder2/folder3', file.txt"
        })
        void shouldNormalizePathsCorrectly(String input, String expectedDirectory, String expectedFilename) {
            // when
            ParsedPath result = queryPathParser.parse(input);

            // then
            assertThat(result.directory()).isEqualTo(expectedDirectory);
            assertThat(result.filename()).isEqualTo(expectedFilename);
        }

        @Test
        @DisplayName("Should remove trailing separator from directory path")
        void shouldRemoveTrailingSeparatorFromDirectoryPath() {
            // Act
            ParsedPath result = queryPathParser.parse("folder1/folder2/");

            // Assert
            assertThat(result.directory()).isEqualTo("folder1/folder2");
            assertThat(result.directory()).doesNotEndWith("/");
        }

        @Test
        @DisplayName("Should remove trailing separator from parent path in file path")
        void shouldRemoveTrailingSeparatorFromParentPath() {
            // Act
            ParsedPath result = queryPathParser.parse("folder1/folder2/file.txt");

            // Assert
            // FilenameUtils.getPath() returns "folder1/folder2/", we remove trailing /
            assertThat(result.directory()).isEqualTo("folder1/folder2");
            assertThat(result.directory()).doesNotEndWith("/");
        }

        @Test
        @DisplayName("Should handle path with whitespace and Windows separators")
        void shouldHandlePathWithWhitespaceAndWindowsSeparators() {
            // Act
            ParsedPath result = queryPathParser.parse("  folder1\\folder2\\file.txt  ");

            // Assert
            assertThat(result.directory()).isEqualTo("folder1/folder2");
            assertThat(result.filename()).isEqualTo("file.txt");
        }
    }

    @Nested
    @DisplayName("parse - Edge cases")
    class ParseEdgeCasesTests {

        @Test
        @DisplayName("Should handle single character directory")
        void shouldHandleSingleCharacterDirectory() {
            // when
            ParsedPath result = queryPathParser.parse("a/");

            // then
            assertThat(result.directory()).isEqualTo("a");
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("Should handle single character filename")
        void shouldHandleSingleCharacterFilename() {
            // when
            ParsedPath result = queryPathParser.parse("f");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isEqualTo("f");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        @DisplayName("Should handle file with only extension")
        void shouldHandleFileWithOnlyExtension() {
            // when
            ParsedPath result = queryPathParser.parse(".gitignore");

            // then
            assertThat(result.directory()).isEmpty();
            assertThat(result.filename()).isEqualTo(".gitignore");
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        @DisplayName("Should handle multiple trailing slashes")
        void shouldHandleMultipleTrailingSlashes() {
            // when
            ParsedPath result = queryPathParser.parse("folder///");

            // then
            assertThat(result.directory()).isEqualTo("folder");
            assertThat(result.filename()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }
    }

    @Nested
    @DisplayName("parse - Validation")
    class ParseValidationTests {

        @Test
        @DisplayName("Should call validator with original path")
        void shouldCallValidatorWithOriginalPath() {
            // given
            String path = "folder/file.txt";

            // when
            queryPathParser.parse(path);

            // then
            verify(pathValidator).validateDirectoryPath(path);
        }

        @Test
        @DisplayName("Should call validator before parsing")
        void shouldCallValidatorBeforeParsing() {
            // given
            doThrow(new InvalidPathException("Invalid path"))
                    .when(pathValidator).validateDirectoryPath(anyString());

            // when & then
            assertThatThrownBy(() -> queryPathParser.parse("../file.txt"))
                    .isInstanceOf(InvalidPathException.class);

            verify(pathValidator).validateDirectoryPath("../file.txt");
        }

        @Test
        @DisplayName("Should propagate validation exception for path traversal")
        void shouldPropagateValidationExceptionForPathTraversal() {
            // given
            doThrow(new InvalidPathException("Path traversal detected"))
                    .when(pathValidator).validateDirectoryPath("../folder/file.txt");

            // when & then
            assertThatThrownBy(() -> queryPathParser.parse("../folder/file.txt"))
                    .isInstanceOf(InvalidPathException.class)
                    .hasMessageContaining("Path traversal detected");
        }

        @Test
        @DisplayName("Should propagate validation exception for invalid characters")
        void shouldPropagateValidationExceptionForInvalidCharacters() {
            // given
            doThrow(new InvalidPathException("Invalid characters"))
                    .when(pathValidator).validateDirectoryPath("folder@name/file.txt");

            // when & then
            assertThatThrownBy(() -> queryPathParser.parse("folder@name/file.txt"))
                    .isInstanceOf(InvalidPathException.class)
                    .hasMessageContaining("Invalid characters");
        }

        @Test
        @DisplayName("Should validate null path")
        void shouldValidateNullPath() {
            // when
            queryPathParser.parse(null);

            // then
            verify(pathValidator).validateDirectoryPath(null);
        }

        @Test
        @DisplayName("Should validate empty path")
        void shouldValidateEmptyPath() {
            // when
            queryPathParser.parse("");

            // then
            verify(pathValidator).validateDirectoryPath("");
        }

        @Test
        @DisplayName("Should validate whitespace path")
        void shouldValidateWhitespacePath() {
            // when
            queryPathParser.parse("   ");

            // then
            verify(pathValidator).validateDirectoryPath("   ");
        }
    }

    @Nested
    @DisplayName("ParsedPath helper methods")
    class ParsedPathHelperMethodsTests {

        @Test
        @DisplayName("isFile should return true for file path")
        void isFileShouldReturnTrueForFilePath() {
            // when
            ParsedPath result = queryPathParser.parse("folder/file.txt");

            // then
            assertThat(result.isFile()).isTrue();
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        @DisplayName("isDirectory should return true for directory path")
        void isDirectoryShouldReturnTrueForDirectoryPath() {
            // when
            ParsedPath result = queryPathParser.parse("folder/");

            // then
            assertThat(result.isDirectory()).isTrue();
            assertThat(result.isFile()).isFalse();
        }

        @Test
        @DisplayName("isDirectory should return true for root path")
        void isDirectoryShouldReturnTrueForRootPath() {
            // when
            ParsedPath result = queryPathParser.parse("");

            // then
            assertThat(result.isDirectory()).isTrue();
            assertThat(result.isFile()).isFalse();
        }
    }
}
