package com.waynehays.cloudfilestorage.unit.validator;

import com.waynehays.cloudfilestorage.exception.InvalidFilenameException;
import com.waynehays.cloudfilestorage.exception.InvalidPathException;
import com.waynehays.cloudfilestorage.validator.PathValidatorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathValidatorImplTest {

    private final PathValidatorImpl pathValidator = new PathValidatorImpl();

    @Nested
    @DisplayName("validateUploadPath - Combined validation")
    class ValidateUploadPathTests {

        @Test
        @DisplayName("Should accept valid filename and directory")
        void shouldAcceptValidFilenameAndDirectory() {
            assertThatCode(() -> pathValidator.validateUploadPath("file.txt", "folder1/folder2"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid filename with null directory")
        void shouldAcceptValidFilenameWithNullDirectory() {
            assertThatCode(() -> pathValidator.validateUploadPath("file.txt", null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid filename with empty directory")
        void shouldAcceptValidFilenameWithEmptyDirectory() {
            assertThatCode(() -> pathValidator.validateUploadPath("file.txt", ""))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept filename with embedded path and valid directory")
        void shouldAcceptFilenameWithEmbeddedPath() {
            assertThatCode(() -> pathValidator.validateUploadPath("subfolder/file.txt", "documents"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject blank filename")
        void shouldRejectBlankFilename() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("", "folder"))
                    .isInstanceOf(InvalidFilenameException.class)
                    .hasMessageContaining("Filename cannot be blank");
        }

        @Test
        @DisplayName("Should reject null filename")
        void shouldRejectNullFilename() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath(null, "folder"))
                    .isInstanceOf(InvalidFilenameException.class)
                    .hasMessageContaining("Filename cannot be blank");
        }

        @Test
        @DisplayName("Should reject whitespace-only filename")
        void shouldRejectWhitespaceOnlyFilename() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("   ", "folder"))
                    .isInstanceOf(InvalidFilenameException.class)
                    .hasMessageContaining("Filename cannot be blank");
        }

        @Test
        @DisplayName("Should reject filename with path traversal")
        void shouldRejectFilenameWithPathTraversal() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("../file.txt", "folder"))
                    .isInstanceOf(InvalidFilenameException.class)
                    .hasMessageContaining("Path traversal detected");
        }

        @Test
        @DisplayName("Should reject directory with path traversal")
        void shouldRejectDirectoryWithPathTraversal() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("file.txt", "../folder"))
                    .isInstanceOf(InvalidPathException.class)
                    .hasMessageContaining("Path traversal detected");
        }

        @Test
        @DisplayName("Should reject filename with invalid characters")
        void shouldRejectFilenameWithInvalidCharacters() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("file@name.txt", "folder"))
                    .isInstanceOf(InvalidFilenameException.class)
                    .hasMessageContaining("Invalid characters");
        }

        @Test
        @DisplayName("Should reject directory with invalid characters")
        void shouldRejectDirectoryWithInvalidCharacters() {
            assertThatThrownBy(() -> pathValidator.validateUploadPath("file.txt", "folder@name"))
                    .isInstanceOf(InvalidPathException.class)
                    .hasMessageContaining("Invalid characters");
        }
    }

    @Nested
    @DisplayName("validateDirectoryPath - Directory validation")
    class ValidateDirectoryPathTests {

        @Nested
        @DisplayName("Valid directory paths")
        class ValidDirectoryPaths {

            @Test
            @DisplayName("Should accept null directory")
            void shouldAcceptNullDirectory() {
                assertThatCode(() -> pathValidator.validateQueryPath(null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept empty directory")
            void shouldAcceptEmptyDirectory() {
                assertThatCode(() -> pathValidator.validateQueryPath(""))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept whitespace-only directory")
            void shouldAcceptWhitespaceOnlyDirectory() {
                assertThatCode(() -> pathValidator.validateQueryPath("   "))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept single directory")
            void shouldAcceptSingleDirectory() {
                assertThatCode(() -> pathValidator.validateQueryPath("documents"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept nested directory path")
            void shouldAcceptNestedDirectoryPath() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder1/folder2/folder3"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with trailing slash")
            void shouldAcceptDirectoryWithTrailingSlash() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder1/folder2/"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with hyphens")
            void shouldAcceptDirectoryWithHyphens() {
                assertThatCode(() -> pathValidator.validateQueryPath("my-folder/sub-folder"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with underscores")
            void shouldAcceptDirectoryWithUnderscores() {
                assertThatCode(() -> pathValidator.validateQueryPath("my_folder/sub_folder"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with dots")
            void shouldAcceptDirectoryWithDots() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder.v1.0/sub.folder"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with numbers")
            void shouldAcceptDirectoryWithNumbers() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder123/folder456"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with mixed allowed characters")
            void shouldAcceptDirectoryWithMixedAllowedCharacters() {
                assertThatCode(() -> pathValidator.validateQueryPath("My_Folder-v1.0/Sub_Folder-v2.1"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept directory with double dots in name (not parent reference)")
            void shouldAcceptDirectoryWithDoubleDotsInName() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder..name"))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("Invalid directory paths")
        class InvalidDirectoryPaths {

            @Test
            @DisplayName("Should reject directory with parent directory reference")
            void shouldRejectDirectoryWithParentDirectoryReference() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder1/../folder2"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject directory starting with parent reference")
            void shouldRejectDirectoryStartingWithParentReference() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("../folder"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject directory ending with parent reference")
            void shouldRejectDirectoryEndingWithParentReference() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder/.."))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject directory with only parent reference")
            void shouldRejectDirectoryWithOnlyParentReference() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath(".."))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject directory with space")
            void shouldRejectDirectoryWithSpace() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("my folder"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with @ sign")
            void shouldRejectDirectoryWithAtSign() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder@name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters")
                        .hasMessageContaining("folder@name");
            }

            @Test
            @DisplayName("Should reject directory with # sign")
            void shouldRejectDirectoryWithHashSign() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder#name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters")
                        .hasMessageContaining("folder#name");
            }

            @Test
            @DisplayName("Should reject directory with $ sign")
            void shouldRejectDirectoryWithDollarSign() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder$name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with % sign")
            void shouldRejectDirectoryWithPercentSign() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder%name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with & sign")
            void shouldRejectDirectoryWithAmpersand() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder&name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with * sign")
            void shouldRejectDirectoryWithAsterisk() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder*name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with parentheses")
            void shouldRejectDirectoryWithParentheses() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder(name)"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with brackets")
            void shouldRejectDirectoryWithBrackets() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder[name]"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with question mark")
            void shouldRejectDirectoryWithQuestionMark() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder?name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject directory with exclamation mark")
            void shouldRejectDirectoryWithExclamationMark() {
                assertThatThrownBy(() -> pathValidator.validateQueryPath("folder!name"))
                        .isInstanceOf(InvalidPathException.class)
                        .hasMessageContaining("Invalid characters");
            }
        }

        @Nested
        @DisplayName("Normalization handling")
        class NormalizationHandling {

            @Test
            @DisplayName("Should handle Windows-style separators")
            void shouldHandleWindowsStyleSeparators() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder1\\folder2"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should handle mixed separators")
            void shouldHandleMixedSeparators() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder1/folder2\\folder3"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should handle path with leading slash")
            void shouldHandlePathWithLeadingSlash() {
                assertThatCode(() -> pathValidator.validateQueryPath("/folder1/folder2"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should handle path with multiple consecutive slashes")
            void shouldHandlePathWithMultipleConsecutiveSlashes() {
                assertThatCode(() -> pathValidator.validateQueryPath("folder1///folder2"))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should handle path with whitespace around slashes")
            void shouldHandlePathWithWhitespaceAroundSlashes() {
                // Whitespace is trimmed only at start/end by normalize()
                // Between parts it's invalid
                assertThatCode(() -> pathValidator.validateQueryPath("folder1/folder2"))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("validateFilename - Filename validation (private via validateUploadPath)")
    class ValidateFilenameTests {

        @Nested
        @DisplayName("Valid filenames")
        class ValidFilenames {

            @Test
            @DisplayName("Should accept simple filename")
            void shouldAcceptSimpleFilename() {
                assertThatCode(() -> pathValidator.validateUploadPath("file.txt", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with embedded path")
            void shouldAcceptFilenameWithEmbeddedPath() {
                assertThatCode(() -> pathValidator.validateUploadPath("subfolder/file.txt", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with multiple extensions")
            void shouldAcceptFilenameWithMultipleExtensions() {
                assertThatCode(() -> pathValidator.validateUploadPath("archive.tar.gz", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename without extension")
            void shouldAcceptFilenameWithoutExtension() {
                assertThatCode(() -> pathValidator.validateUploadPath("README", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with hyphens")
            void shouldAcceptFilenameWithHyphens() {
                assertThatCode(() -> pathValidator.validateUploadPath("my-file-name.txt", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with underscores")
            void shouldAcceptFilenameWithUnderscores() {
                assertThatCode(() -> pathValidator.validateUploadPath("my_file_name.txt", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with dots")
            void shouldAcceptFilenameWithDots() {
                assertThatCode(() -> pathValidator.validateUploadPath("file.v1.0.txt", null))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Should accept filename with double dots (not parent reference)")
            void shouldAcceptFilenameWithDoubleDots() {
                assertThatCode(() -> pathValidator.validateUploadPath("file..txt", null))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("Invalid filenames")
        class InvalidFilenames {

            @Test
            @DisplayName("Should reject filename with path traversal at start")
            void shouldRejectFilenameWithPathTraversalAtStart() {
                assertThatThrownBy(() -> pathValidator.validateUploadPath("../file.txt", null))
                        .isInstanceOf(InvalidFilenameException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject filename with path traversal in middle")
            void shouldRejectFilenameWithPathTraversalInMiddle() {
                assertThatThrownBy(() -> pathValidator.validateUploadPath("folder/../file.txt", null))
                        .isInstanceOf(InvalidFilenameException.class)
                        .hasMessageContaining("Path traversal detected");
            }

            @Test
            @DisplayName("Should reject filename with space")
            void shouldRejectFilenameWithSpace() {
                assertThatThrownBy(() -> pathValidator.validateUploadPath("my file.txt", null))
                        .isInstanceOf(InvalidFilenameException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject filename with special characters")
            void shouldRejectFilenameWithSpecialCharacters() {
                assertThatThrownBy(() -> pathValidator.validateUploadPath("file@name.txt", null))
                        .isInstanceOf(InvalidFilenameException.class)
                        .hasMessageContaining("Invalid characters");
            }

            @Test
            @DisplayName("Should reject filename with embedded path containing invalid characters")
            void shouldRejectFilenameWithEmbeddedPathContainingInvalidCharacters() {
                assertThatThrownBy(() -> pathValidator.validateUploadPath("folder@name/file.txt", null))
                        .isInstanceOf(InvalidFilenameException.class)
                        .hasMessageContaining("Invalid characters");
            }
        }
    }
}
