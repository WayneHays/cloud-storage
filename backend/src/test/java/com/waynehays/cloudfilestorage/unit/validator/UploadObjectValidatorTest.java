package com.waynehays.cloudfilestorage.unit.validator;

import com.waynehays.cloudfilestorage.config.properties.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.validator.UploadObjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MultipartFileValidator unit tests")
class UploadObjectValidatorTest {

    private UploadObjectValidator validator;

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L;
    private static final long VALID_FILE_SIZE = 1024L;

    @BeforeEach
    void setUp() {
        ResourceLimitsProperties properties = new ResourceLimitsProperties(
                500, 200, DataSize.ofMegabytes(500)
        );
        validator = new UploadObjectValidator(properties);
    }

    private UploadObjectDto dto(String filename, String fullPath, long size) {
        return new UploadObjectDto(filename, filename, "docs/", fullPath, size,
                "application/octet-stream", InputStream::nullInputStream);
    }

    @Nested
    @DisplayName("Valid filenames")
    class ValidFilenames {

        @ParameterizedTest
        @ValueSource(strings = {"file.txt", "report-2024.pdf", "my_document.docx", "Отчёт.xlsx", "photo 001.jpg"})
        @DisplayName("Should accept valid filenames")
        void shouldAcceptValidFilenames(String filename) {
            // when
            Optional<String> result = validator.validate(dto(filename, "docs/" + filename, VALID_FILE_SIZE));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid filenames")
    class InvalidFilenames {

        @Test
        @DisplayName("Should reject blank filename")
        void shouldRejectBlankFilename() {
            // when
            Optional<String> result = validator.validate(dto("", "docs/", VALID_FILE_SIZE));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("invalid characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"file@name.txt", "test%file.txt", "file#1.txt"})
        @DisplayName("Should reject filenames with special characters")
        void shouldRejectSpecialCharacters(String filename) {
            // when
            Optional<String> result = validator.validate(dto(filename, "docs/" + filename, VALID_FILE_SIZE));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("invalid characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {".hidden", ".gitignore", "..secret"})
        @DisplayName("Should reject hidden filenames")
        void shouldRejectHiddenFilenames(String filename) {
            // when
            Optional<String> result = validator.validate(dto(filename, "docs/" + filename, VALID_FILE_SIZE));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("invalid characters");
        }

        @Test
        @DisplayName("Should reject filename exceeding max length")
        void shouldRejectExceedingMaxLength() {
            // given
            String longFilename = "a".repeat(201) + ".txt";

            // when
            Optional<String> result = validator.validate(dto(longFilename, "docs/" + longFilename, VALID_FILE_SIZE));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("max filename length");
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

            // when
            Optional<String> result = validator.validate(dto("file.txt", fullPath, VALID_FILE_SIZE));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("max length");
        }

        @Test
        @DisplayName("Should accept full path within max length")
        void shouldAcceptWithinMaxLength() {
            // when
            Optional<String> result = validator.validate(dto("file.txt", "docs/work/file.txt", VALID_FILE_SIZE));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("File size")
    class FileSize {

        @Test
        @DisplayName("Should accept file within size limit")
        void shouldAcceptFileWithinLimit() {
            // when
            Optional<String> result = validator.validate(dto("file.txt", "docs/file.txt", MAX_FILE_SIZE_BYTES));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void shouldRejectFileExceedingLimit() {
            // given
            long oversizedFile = MAX_FILE_SIZE_BYTES + 1;

            // when
            Optional<String> result = validator.validate(dto("file.txt", "docs/file.txt", oversizedFile));

            // then
            assertThat(result).isPresent()
                    .get().asString().contains("max size");
        }
    }
}
