package com.waynehays.cloudfilestorage.unit.validator;

import com.waynehays.cloudfilestorage.validator.UploadPathValidatorImpl;
import org.apache.tomcat.util.http.fileupload.InvalidFileNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.InvalidPathException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UploadPathValidatorTest {

    private final UploadPathValidatorImpl validator = new UploadPathValidatorImpl();

    @Nested
    @DisplayName("Filename validation tests")
    class FilenameValidationTests {

        private static Stream<Arguments> provideSuccessValidation() {
            return Stream.of(
                    Arguments.of("file.txt"),
                    Arguments.of("file.tar.gz"),
                    Arguments.of("folder1/folder2/file.txt"),
                    Arguments.of("file_v1.0.txt"),
                    Arguments.of("file..txt")
            );
        }

        private static Stream<Arguments> provideFailedValidation() {
            return Stream.of(
                    Arguments.of(""),
                    Arguments.of((Object) null),
                    Arguments.of("   "),
                    Arguments.of(".."),
                    Arguments.of("folder/../file.txt"),
                    Arguments.of("../file.txt"),
                    Arguments.of("my file.txt"),
                    Arguments.of("file@name.txt"),
                    Arguments.of("file#name.txt"),
                    Arguments.of("folder\\\\file.txt"),
                    Arguments.of("folder//file.txt"),
                    Arguments.of("folder//file.txt")
            );
        }

        @ParameterizedTest
        @MethodSource("provideSuccessValidation")
        void givenFilename_whenValidate_thenSuccess(String filename) {
            assertDoesNotThrow(() -> {
                validator.validate(filename, null);
            });
        }

        @ParameterizedTest
        @MethodSource("provideFailedValidation")
        void givenFilename_whenValidate_thenFail(String filename) {
            assertThatThrownBy(() -> {
                validator.validate(filename, null);
            }).isInstanceOf(InvalidFileNameException.class);
        }
    }

    @Nested
    @DisplayName("Directory validation tests")
    class DirectoryValidationTests {

        private static Stream<Arguments> provideSuccessValidation() {
            return Stream.of(
                    Arguments.of("file.txt", "docs"),
                    Arguments.of("file.txt", "docs/work"),
                    Arguments.of("file.txt", ""),
                    Arguments.of("file.txt", "my-folder_v1"),
                    Arguments.of("file.txt", ""),
                    Arguments.of("file.txt", "/docs"),
                    Arguments.of("file.txt", "docs/")
            );
        }

        private static Stream<Arguments> provideFailValidation() {
            return Stream.of(
                    Arguments.of("file.txt", ".."),
                    Arguments.of("file.txt", "folder/../docs"),
                    Arguments.of("file.txt", "../docs"),
                    Arguments.of("file.txt", "my@folder")
            );
        }

        @ParameterizedTest
        @MethodSource("provideSuccessValidation")
        void givenFilenameAndDirectory_whenValidate_thenSuccess(String filename, String directory) {
            assertDoesNotThrow(() -> validator.validate(filename, directory));
        }

        @ParameterizedTest
        @MethodSource("provideFailValidation")
        void givenFilenameAndDirectory_whenValidate_thenFail(String filename, String directory) {
            assertThatThrownBy(() -> {
                validator.validate(filename, directory);
            }).isInstanceOf(InvalidPathException.class);
        }
    }

    @Nested
    @DisplayName("Combined filename and directory validation tests")
    class CombinedValidationTests {

        @Test
        @DisplayName("Should accept valid filename with valid directory")
        void shouldAcceptValidFilenameWithValidDirectory() {
            //given
            String filename = "folder/file.txt";
            String directory = "docs/work";

            // when & then
            assertDoesNotThrow(() -> {
                validator.validate(filename, directory);
            });
        }

        @Test
        @DisplayName("Should reject when filename valid but directory has ..")
        void shouldRejectWhenFilenameValidButDirectoryInvalid() {
            //given
            String filename = "file.txt";
            String directory = "../docs";

            // when & then
            assertThatThrownBy(() -> {
                validator.validate(filename, directory);
            }).isInstanceOf(InvalidPathException.class);
        }

        @Test
        @DisplayName("Should reject when directory valid but filename has ..")
        void shouldRejectWhenDirectoryValidButFilenameInvalid() {
            //given
            String filename = "../file.txt";
            String directory = "docs";

            // when & then
            assertThatThrownBy(() -> {
                validator.validate(filename, directory);
            }).isInstanceOf(InvalidFileNameException.class);
        }
    }
}
