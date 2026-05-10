package com.waynehays.cloudfilestorage.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Nested
    class IsDirectory {

        @Test
        @DisplayName("Should return true when path ends with slash")
        void shouldReturnTrue_whenPathEndsWithSlash() {
            assertThat(PathUtils.isDirectory("docs/")).isTrue();
        }

        @Test
        @DisplayName("Should return false when path does not end with slash")
        void shouldReturnFalse_whenPathNotEndsWithSlash() {
            assertThat(PathUtils.isDirectory("file.txt")).isFalse();
        }
    }

    @Nested
    class IsFile {

        @Test
        @DisplayName("Should return true when path is a file")
        void shouldReturnTrueForFilePath() {
            assertThat(PathUtils.isFile("file.txt")).isTrue();
        }

        @Test
        @DisplayName("Should return false when path is a directory")
        void shouldReturnFalseForDirectoryPath() {
            assertThat(PathUtils.isFile("docs/")).isFalse();
        }
    }

    @Nested
    class EnsureTrailingSlash {

        @Test
        @DisplayName("Should add slash when missing")
        void shouldAddSlashWhenMissing() {
            assertThat(PathUtils.ensureTrailingSlash("docs")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should not add slash when already present")
        void shouldNotAddSlashWhenPresent() {
            assertThat(PathUtils.ensureTrailingSlash("docs/")).isEqualTo("docs/");
        }
    }

    @Nested
    class GetAllAncestorDirectories {

        @Test
        @DisplayName("Should return all ancestor directories for a file path")
        void shouldReturnAncestorsForFilePath() {
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/file.txt");

            assertThat(result).containsExactlyInAnyOrder("a/", "a/b/", "a/b/c/");
        }

        @Test
        @DisplayName("Should return all ancestor directories including self for a directory path")
        void shouldReturnAncestorsForDirectoryPath() {
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/");

            assertThat(result).containsExactlyInAnyOrder("a/", "a/b/", "a/b/c/");
        }

        @Test
        @DisplayName("Should return empty set for blank path")
        void shouldReturnEmptySetForBlankPath() {
            assertThat(PathUtils.getAllAncestorDirectories("")).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for a root-level file")
        void shouldReturnEmptySetForRootLevelFile() {
            assertThat(PathUtils.getAllAncestorDirectories("file.txt")).isEmpty();
        }
    }

    @Nested
    class NormalizePath {

        @Test
        @DisplayName("Should convert path to lowercase")
        void shouldConvertToLowercase() {
            assertThat(PathUtils.normalizePath("Docs/MyFile.TXT")).isEqualTo("docs/myfile.txt");
        }

        @Test
        @DisplayName("Should keep already lowercase path unchanged")
        void shouldKeepLowercaseUnchanged() {
            assertThat(PathUtils.normalizePath("docs/file.txt")).isEqualTo("docs/file.txt");
        }
    }

    @Nested
    class NormalizeSeparators {

        @Test
        @DisplayName("Should replace backslashes with forward slashes")
        void shouldConvertBackslashesToForwardSlashes() {
            assertThat(PathUtils.normalizeSeparators("docs\\sub\\file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }

        @Test
        @DisplayName("Should keep forward slashes unchanged")
        void shouldKeepForwardSlashesUnchanged() {
            assertThat(PathUtils.normalizeSeparators("docs/sub/file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }
    }

    @Nested
    class RemoveTrailingSlash {

        @Test
        @DisplayName("Should remove trailing slash")
        void shouldRemoveTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs/")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return path unchanged when no trailing slash")
        void shouldReturnSameWhenNoTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return empty string for empty input")
        void shouldHandleEmptyString() {
            assertThat(PathUtils.removeTrailingSlash("")).isEmpty();
        }
    }

    @Nested
    class GetParentPath {

        @Test
        @DisplayName("Should return parent directory path for a file")
        void shouldExtractParentForFile() {
            assertThat(PathUtils.getParentPath("docs/file.txt")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should return parent directory path for a nested directory")
        void shouldExtractParentForDirectory() {
            assertThat(PathUtils.getParentPath("docs/sub/")).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should return parent path for a deeply nested file")
        void shouldExtractParentForNestedFile() {
            assertThat(PathUtils.getParentPath("a/b/c/file.txt")).isEqualTo("a/b/c/");
        }

        @Test
        @DisplayName("Should return empty string for a root-level file")
        void shouldReturnEmptyForRootLevelFile() {
            assertThat(PathUtils.getParentPath("file.txt")).isEmpty();
        }

        @Test
        @DisplayName("Should return empty string for a root-level directory")
        void shouldReturnEmptyForRootLevelDirectory() {
            assertThat(PathUtils.getParentPath("docs/")).isEmpty();
        }
    }

    @Nested
    class GetName {

        @Test
        @DisplayName("Should extract filename from a file path")
        void shouldExtractFilename() {
            assertThat(PathUtils.getName("docs/file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should extract directory name without trailing slash")
        void shouldExtractDirectoryName() {
            assertThat(PathUtils.getName("docs/sub/")).isEqualTo("sub");
        }

        @Test
        @DisplayName("Should extract filename from a root-level file")
        void shouldExtractRootLevelFilename() {
            assertThat(PathUtils.getName("file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should extract directory name from a root-level directory without trailing slash")
        void shouldExtractRootLevelDirectoryName() {
            assertThat(PathUtils.getName("docs/")).isEqualTo("docs");
        }
    }

    @Nested
    class Combine {

        @Test
        @DisplayName("Should combine base directory and filename")
        void shouldCombineTwoPaths() {
            assertThat(PathUtils.combine("docs", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        @DisplayName("Should strip trailing slash from base before combining")
        void shouldHandleTrailingSlashOnBase() {
            assertThat(PathUtils.combine("docs/", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        @DisplayName("Should strip trailing slash from sub before combining")
        void shouldHandleTrailingSlashOnSub() {
            assertThat(PathUtils.combine("docs", "sub/")).isEqualTo("docs/sub");
        }

        @Test
        @DisplayName("Should return sub when base is empty")
        void shouldReturnSubWhenBaseEmpty() {
            assertThat(PathUtils.combine("", "file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should return base when sub is empty")
        void shouldReturnBaseWhenSubEmpty() {
            assertThat(PathUtils.combine("docs", "")).isEqualTo("docs");
        }

        @Test
        @DisplayName("Should return empty string when both are empty")
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