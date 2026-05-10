package com.waynehays.cloudfilestorage.files.api.validation;

import com.waynehays.cloudfilestorage.files.operation.upload.config.ResourceLimitsProperties;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

class PathValidatorTest {

    private PathValidator validator;

    @BeforeEach
    void setUp() {
        ResourceLimitsProperties properties = new ResourceLimitsProperties(500, 200,
                DataSize.ofMegabytes(500));
        validator = new PathValidator(properties);
    }

    @Nested
    class FilePaths {

        @BeforeEach
        void init() {
            ValidPath annotation = createAnnotation(false);
            validator.initialize(annotation);
        }

        @ParameterizedTest
        @ValueSource(strings = {"file.txt", "docs/file.txt", "docs/work/report.pdf", "Документы/файл.txt"})
        @DisplayName("Should accept valid file paths")
        void shouldAcceptValidFilePaths(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isTrue();
        }

        @Test
        @DisplayName("Should accept blank path")
        void shouldAcceptBlankPath() {
            // when & then
            assertThat(validator.isValid("", null)).isTrue();
            assertThat(validator.isValid(null, null)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"../hack", "docs/../secret", "./file.txt"})
        @DisplayName("Should reject path traversal")
        void shouldRejectPathTraversal(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"file@name.txt", "docs/file#1.txt", "test%file"})
        @DisplayName("Should reject paths with special characters")
        void shouldRejectSpecialCharacters(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isFalse();
        }

        @Test
        @DisplayName("Should reject path exceeding max length")
        void shouldRejectExceedingMaxLength() {
            // given
            String longPath = "a".repeat(501);

            // when & then
            assertThat(validator.isValid(longPath, null)).isFalse();
        }

        @Test
        @DisplayName("Should reject path with empty segment")
        void shouldRejectEmptySegment() {
            // when & then
            assertThat(validator.isValid("docs//file.txt", null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {".hidden", "docs/.hidden", ".hidden/file.txt"})
        @DisplayName("Should reject hidden files and directories")
        void shouldRejectHiddenFiles(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isFalse();
        }
    }

    @Nested
    class DirectoryPaths {

        @BeforeEach
        void init() {
            ValidPath annotation = createAnnotation(true);
            validator.initialize(annotation);
        }

        @ParameterizedTest
        @ValueSource(strings = {"docs/", "docs/work/", "Документы/"})
        @DisplayName("Should accept valid directory paths")
        void shouldAcceptValidDirectoryPaths(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"docs", "docs/work", "file.txt"})
        @DisplayName("Should reject directory paths without trailing slash")
        void shouldRejectWithoutTrailingSlash(String path) {
            // when & then
            assertThat(validator.isValid(path, null)).isFalse();
        }

        @Test
        @DisplayName("Should accept blank path for optional directory")
        void shouldAcceptBlankPath() {
            // when & then
            assertThat(validator.isValid("", null)).isTrue();
        }
    }

    private ValidPath createAnnotation(boolean mustBeDirectory) {
        return new ValidPath() {
            @Override
            public String message() {
                return "";
            }

            @Override
            public boolean mustBeDirectory() {
                return mustBeDirectory;
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidPath.class;
            }
        };
    }
}
