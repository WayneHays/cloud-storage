package com.waynehays.cloudfilestorage.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Test
    @DisplayName("Should add slash when missing")
    void shouldAddSlashWhenMissing() {
        assertThat(PathUtils.ensureTrailingSlash("docs")).isEqualTo("docs/");
    }

    @Test
    @DisplayName("Should not add slash when present")
    void shouldNotAddSlashWhenPresent() {
        assertThat(PathUtils.ensureTrailingSlash("docs/")).isEqualTo("docs/");
    }

    @Test
    @DisplayName("Should return true when path ends with slash")
    void shouldReturnTrue_whenPathEndsWithSlash() {
        assertThat(PathUtils.isDirectory("docs/")).isTrue();
    }

    @Test
    @DisplayName("Should return false when path not ends with slash")
    void shouldReturnFalse_whenPathNotEndsWithSlash() {
        assertThat(PathUtils.isDirectory("file.txt")).isFalse();
    }

    @Test
    @DisplayName("Should return true when path is file")
    void shouldReturnTrueForFilePath() {
        assertThat(PathUtils.isFile("file.txt")).isTrue();
    }

    @Test
    @DisplayName("Should return false when path is directory")
    void shouldReturnFalseForDirectoryPath() {
        assertThat(PathUtils.isFile("docs/")).isFalse();
    }

    @Nested
    class GetAllDirectories {

        @Test
        @DisplayName("Should return all separated directories from file path")
        void shouldReturnAncestorsForFilePath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/file.txt");

            // then
            assertThat(result).containsExactly("a/", "a/b/", "a/b/c/");
        }

        @Test
        @DisplayName("Should return all separated directories from directory path")
        void shouldReturnAncestorsForDirectoryPath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/");

            // then
            assertThat(result).containsExactly("a/", "a/b/", "a/b/c/");
        }

        @Test
        @DisplayName("Should return empty set for blank path")
        void shouldReturnEmptySetForBlankPath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for single file in root")
        void shouldReturnEmptySetForSingleFile() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("file.txt");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class NormalizeSeparators {

        @Test
        @DisplayName("Should replace all backslashes to forward slashes")
        void shouldConvertBackslashesToForwardSlashes() {
            assertThat(PathUtils.normalizeSeparators("docs\\sub\\file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }

        @Test
        @DisplayName("Should keep forward slashes the same")
        void shouldKeepForwardSlashesUnchanged() {
            assertThat(PathUtils.normalizeSeparators("docs/sub/file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }
    }

    @Nested
    class RemoveTrailingSlash {

        @Test
        @DisplayName("Should remove trailing slash from path when path ends with slash")
        void shouldRemoveTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs/")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return the same path when path not ends with slash")
        void shouldReturnSameWhenNoTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return empty string when input is empty string")
        void shouldHandleEmptyString() {
            assertThat(PathUtils.removeTrailingSlash("")).isEmpty();
        }
    }

    @Nested
    class ExtractParentPath {

        @Test
        @DisplayName("Should extract path to parent directory from file path")
        void shouldExtractParentForFile() {
            assertThat(PathUtils.extractParentPath("docs/file.txt")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should extract path to parent directory from directory path")
        void shouldExtractParentForDirectory() {
            assertThat(PathUtils.extractParentPath("docs/sub/")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should extract path to parent directory from nested directories file path")
        void shouldExtractParentForNestedFile() {
            assertThat(PathUtils.extractParentPath("a/b/c/file.txt")).isEqualTo("a/b/c/");
        }

        @Test
        @DisplayName("Should return empty for root-level file")
        void shouldReturnEmptyForRootLevelFile() {
            assertThat(PathUtils.extractParentPath("file.txt")).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for root-level directory")
        void shouldReturnEmptyForRootLevelDirectory() {
            assertThat(PathUtils.extractParentPath("docs/")).isEmpty();
        }
    }

    @Nested
    class ExtractFilename {

        @Test
        @DisplayName("Should extract filename from file path")
        void shouldExtractFilename() {
            assertThat(PathUtils.extractName("docs/file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should extract directory name from directory path")
        void shouldExtractDirectoryName() {
            assertThat(PathUtils.extractName("docs/sub/")).isEqualTo("sub/");
        }

        @Test
        @DisplayName("Should extract filename from root-level file")
        void shouldExtractRootLevelFilename() {
            assertThat(PathUtils.extractName("file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should extract directory name from root-level directory")
        void shouldExtractRootLevelDirectoryName() {
            assertThat(PathUtils.extractName("docs/")).isEqualTo("docs/");
        }
    }

    @Nested
    class Combine {

        @Test
        @DisplayName("Should return full path for file from base directory and filename")
        void shouldCombineTwoPaths() {
            assertThat(PathUtils.combine("docs", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        @DisplayName("Should correctly handle trailing slash for base directory and filename")
        void shouldHandleTrailingSlashOnBase() {
            assertThat(PathUtils.combine("docs/", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        @DisplayName("Should return correct path to nested directory from base directory and sub directory")
        void shouldHandleTrailingSlashOnSub() {
            assertThat(PathUtils.combine("docs", "sub/")).isEqualTo("docs/sub");
        }

        @Test
        @DisplayName("Should return correct path to root-level file")
        void shouldReturnSubWhenBaseEmpty() {
            assertThat(PathUtils.combine("", "file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should return correct path to root-level file when filename is empty")
        void shouldReturnBaseWhenSubEmpty() {
            assertThat(PathUtils.combine("docs", "")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return empty string when base directory and filename are empty")
        void shouldHandleBothEmpty() {
            assertThat(PathUtils.combine("", "")).isEmpty();
        }
    }

    @Nested
    class ToOppositeTypePath {

        @Test
        @DisplayName("Should convert file path to directory path")
        void shouldConvertFileToDirectory() {
            assertThat(PathUtils.toOppositeTypePath("docs")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should convert directory path to file path")
        void shouldConvertDirectoryToFile() {
            assertThat(PathUtils.toOppositeTypePath("docs/")).isEqualTo("docs");
        }
    }
}
