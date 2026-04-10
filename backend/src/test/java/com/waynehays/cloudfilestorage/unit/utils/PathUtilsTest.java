package com.waynehays.cloudfilestorage.unit.utils;

import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        void shouldReturnAllParentDirectories() {
            // when
            List<String> result = PathUtils.getAllDirectories("a/b/c/file.txt");

            // then
            assertThat(result).containsExactly("a", "a/b", "a/b/c", "a/b/c/file.txt");
        }

        @Test
        void shouldHandleDirectoryPath() {
            // when
            List<String> result = PathUtils.getAllDirectories("a/b/");

            // then
            assertThat(result).containsExactly("a", "a/b");
        }

        @Test
        void shouldHandleSingleSegment() {
            // when
            List<String> result = PathUtils.getAllDirectories("docs");

            // then
            assertThat(result).containsExactly("docs");
        }

        @Test
        void shouldReturnEmptyForBlankPath() {
            assertThat(PathUtils.getAllDirectories("")).isEmpty();
            assertThat(PathUtils.getAllDirectories("  ")).isEmpty();
            assertThat(PathUtils.getAllDirectories(null)).isEmpty();
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
            assertThat(PathUtils.extractFilename("docs/sub/")).isEqualTo("sub");
        }

        @Test
        void shouldExtractRootLevelFilename() {
            assertThat(PathUtils.extractFilename("file.txt")).isEqualTo("file.txt");
        }

        @Test
        void shouldExtractRootLevelDirectoryName() {
            assertThat(PathUtils.extractFilename("docs/")).isEqualTo("docs");
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
