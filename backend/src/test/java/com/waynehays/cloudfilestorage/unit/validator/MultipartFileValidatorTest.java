package com.waynehays.cloudfilestorage.unit.validator;

import com.waynehays.cloudfilestorage.config.properties.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.validator.MultipartFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MultipartFileValidator unit tests")
class MultipartFileValidatorTest {

    private MultipartFileValidator validator;

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L;
    private static final long VALID_FILE_SIZE = 1024L;

    @BeforeEach
    void setUp() {
        ResourceLimitsProperties properties = new ResourceLimitsProperties(
                500, 200, DataSize.ofMegabytes(500)
        );
        validator = new MultipartFileValidator(properties);
    }

    @Nested
    @DisplayName("Valid filenames")
    class ValidFilenames {

        @ParameterizedTest
        @ValueSource(strings = {"file.txt", "report-2024.pdf", "my_document.docx", "Отчёт.xlsx", "photo 001.jpg"})
        @DisplayName("Should accept valid filenames")
        void shouldAcceptValidFilenames(String filename) {
            // when & then
            assertThatCode(() -> validator.validate(filename, "docs/" + filename, VALID_FILE_SIZE))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid filenames")
    class InvalidFilenames {

        @Test
        @DisplayName("Should reject blank filename")
        void shouldRejectBlankFilename() {
            // when & then
            assertThatThrownBy(() -> validator.validate("", "docs/", VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("no filename");
        }

        @Test
        @DisplayName("Should reject null filename")
        void shouldRejectNullFilename() {
            // when & then
            assertThatThrownBy(() -> validator.validate(null, "docs/file.txt", VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("no filename");
        }

        @ParameterizedTest
        @ValueSource(strings = {"file@name.txt", "test%file.txt", "file#1.txt"})
        @DisplayName("Should reject filenames with special characters")
        void shouldRejectSpecialCharacters(String filename) {
            // when & then
            assertThatThrownBy(() -> validator.validate(filename, "docs/" + filename, VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("invalid characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {".hidden", ".gitignore", "..secret"})
        @DisplayName("Should reject hidden filenames")
        void shouldRejectHiddenFilenames(String filename) {
            // when & then
            assertThatThrownBy(() -> validator.validate(filename, "docs/" + filename, VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("invalid characters");
        }

        @Test
        @DisplayName("Should reject filename exceeding max length")
        void shouldRejectExceedingMaxLength() {
            // given
            String longFilename = "a".repeat(201) + ".txt";

            // when & then
            assertThatThrownBy(() -> validator.validate(longFilename, "docs/" + longFilename, VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("max length");
        }
    }

    @Nested
    @DisplayName("Path length")
    class PathLength {

        @Test
        @DisplayName("Should reject full path exceeding max length")
        void shouldRejectExceedingMaxPathLength() {
            // given
            String longDir = "a/".repeat(250);
            String fullPath = longDir + "file.txt";

            // when & then
            assertThatThrownBy(() -> validator.validate("file.txt", fullPath, VALID_FILE_SIZE))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("max length");
        }

        @Test
        @DisplayName("Should accept full path within max length")
        void shouldAcceptWithinMaxLength() {
            // given
            String fullPath = "docs/work/file.txt";

            // when & then
            assertThatCode(() -> validator.validate("file.txt", fullPath, VALID_FILE_SIZE))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("File size")
    class FileSize {

        @Test
        @DisplayName("Should accept file within size limit")
        void shouldAcceptFileWithinLimit() {
            // when & then
            assertThatCode(() -> validator.validate("file.txt", "docs/file.txt", MAX_FILE_SIZE_BYTES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void shouldRejectFileExceedingLimit() {
            // given
            long oversizedFile = MAX_FILE_SIZE_BYTES + 1;

            // when & then
            assertThatThrownBy(() -> validator.validate("file.txt", "docs/file.txt", oversizedFile))
                    .isInstanceOf(MultipartValidationException.class)
                    .hasMessageContaining("exceeds max size");
        }
    }
}
