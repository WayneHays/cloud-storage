package com.waynehays.cloudfilestorage.shared.utils;

import com.waynehays.cloudfilestorage.shared.utils.PathUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Nested
    class EnsureTrailingSlash {

        @Test
        void shouldAddSlashWhenMissing() {
            assertThat(PathUtils.ensureTrailingSlash("docs")).isEqualTo("docs/");
        }

        @Test
        void shouldNotAddSlashWhenPresent() {
            assertThat(PathUtils.ensureTrailingSlash("docs/")).isEqualTo("docs/");
        }
    }

    @Nested
    class IsDirectory {

        @Test
        void shouldReturnTrueForDirectoryPath() {
            assertThat(PathUtils.isDirectory("docs/")).isTrue();
        }

        @Test
        void shouldReturnFalseForFilePath() {
            assertThat(PathUtils.isDirectory("file.txt")).isFalse();
        }
    }

    @Nested
    class IsFile {

        @Test
        void shouldReturnTrueForFilePath() {
            assertThat(PathUtils.isFile("file.txt")).isTrue();
        }

        @Test
        void shouldReturnFalseForDirectoryPath() {
            assertThat(PathUtils.isFile("docs/")).isFalse();
        }
    }

    @Nested
    class GetAllDirectories {

        @Test
        void shouldReturnAncestorsForFilePath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/file.txt");

            // then
            assertThat(result).containsExactly("a/", "a/b/", "a/b/c/");
        }

        @Test
        void shouldReturnAncestorsForDirectoryPath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("a/b/c/");

            // then
            assertThat(result).containsExactly("a/", "a/b/", "a/b/c/");
        }

        @Test
        void shouldReturnEmptySetForBlankPath() {
            // when
            Set<String> result = PathUtils.getAllAncestorDirectories("");

            // then
            assertThat(result).isEmpty();
        }

        @Test
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
        void shouldConvertBackslashesToForwardSlashes() {
            assertThat(PathUtils.normalizeSeparators("docs\\sub\\file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }

        @Test
        void shouldKeepForwardSlashesUnchanged() {
            assertThat(PathUtils.normalizeSeparators("docs/sub/file.txt"))
                    .isEqualTo("docs/sub/file.txt");
        }
    }

    @Nested
    class RemoveTrailingSlash {

        @Test
        void shouldRemoveTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs/")).isEqualTo("docs");
        }

        @Test
        void shouldReturnSameWhenNoTrailingSlash() {
            assertThat(PathUtils.removeTrailingSlash("docs")).isEqualTo("docs");
        }

        @Test
        void shouldHandleEmptyString() {
            assertThat(PathUtils.removeTrailingSlash("")).isEmpty();
        }
    }

    @Nested
    class ExtractParentPath {

        @Test
        void shouldExtractParentForFile() {
            assertThat(PathUtils.extractParentPath("docs/file.txt")).isEqualTo("docs/");
        }

        @Test
        void shouldExtractParentForDirectory() {
            assertThat(PathUtils.extractParentPath("docs/sub/")).isEqualTo("docs/");
        }

        @Test
        void shouldExtractParentForNestedFile() {
            assertThat(PathUtils.extractParentPath("a/b/c/file.txt")).isEqualTo("a/b/c/");
        }

        @Test
        void shouldReturnEmptyForRootLevelFile() {
            assertThat(PathUtils.extractParentPath("file.txt")).isEmpty();
        }

        @Test
        void shouldReturnEmptyForRootLevelDirectory() {
            assertThat(PathUtils.extractParentPath("docs/")).isEmpty();
        }
    }

    @Nested
    class ExtractFilename {

        @Test
        void shouldExtractFilename() {
            assertThat(PathUtils.extractFilename("docs/file.txt")).isEqualTo("file.txt");
        }

        @Test
        void shouldExtractDirectoryName() {
            assertThat(PathUtils.extractFilename("docs/sub/")).isEqualTo("sub/");
        }

        @Test
        void shouldExtractRootLevelFilename() {
            assertThat(PathUtils.extractFilename("file.txt")).isEqualTo("file.txt");
        }

        @Test
        void shouldExtractRootLevelDirectoryName() {
            assertThat(PathUtils.extractFilename("docs/")).isEqualTo("docs/");
        }
    }

    @Nested
    class Combine {

        @Test
        void shouldCombineTwoPaths() {
            assertThat(PathUtils.combine("docs", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        void shouldHandleTrailingSlashOnBase() {
            assertThat(PathUtils.combine("docs/", "file.txt")).isEqualTo("docs/file.txt");
        }

        @Test
        void shouldHandleTrailingSlashOnSub() {
            assertThat(PathUtils.combine("docs", "sub/")).isEqualTo("docs/sub");
        }

        @Test
        void shouldReturnSubWhenBaseEmpty() {
            assertThat(PathUtils.combine("", "file.txt")).isEqualTo("file.txt");
        }

        @Test
        void shouldReturnBaseWhenSubEmpty() {
            assertThat(PathUtils.combine("docs", "")).isEqualTo("docs");
        }

        @Test
        void shouldHandleBothEmpty() {
            assertThat(PathUtils.combine("", "")).isEmpty();
        }
    }
}
