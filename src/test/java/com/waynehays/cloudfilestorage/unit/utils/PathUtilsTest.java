package com.waynehays.cloudfilestorage.unit.utils;

import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Test
    @DisplayName("Should normalize separators from Windows to Unix")
    void shouldNormalizeSeparators() {
        String result = PathUtils.normalizeSeparators("folder1\\folder2\\file.txt");
        assertThat(result).isEqualTo("folder1/folder2/file.txt");
    }

    @Test
    @DisplayName("Should remove trailing separator")
    void shouldRemoveTrailingSeparator() {
        String result = PathUtils.removeTrailingSeparator("folder1/folder2/");
        assertThat(result).isEqualTo("folder1/folder2");
    }

    @Test
    @DisplayName("Should not modify path without trailing separator")
    void shouldNotModifyPathWithoutTrailingSeparator() {
        String result = PathUtils.removeTrailingSeparator("folder1/folder2");
        assertThat(result).isEqualTo("folder1/folder2");
    }

    @Test
    @DisplayName("Should detect path ending with separator")
    void shouldDetectPathEndingWithSeparator() {
        assertThat(PathUtils.endsWithSeparator("folder/")).isTrue();
        assertThat(PathUtils.endsWithSeparator("folder")).isFalse();
    }

    @Test
    @DisplayName("Should split path into parts")
    void shouldSplitPathIntoParts() {
        String[] parts = PathUtils.splitIntoParts("folder1/folder2/file.txt");
        assertThat(parts).containsExactly("folder1", "folder2", "file.txt");
    }

    @Test
    @DisplayName("Should extract parent path")
    void shouldExtractParentPath() {
        String parent = PathUtils.extractParentPath("folder1/folder2/file.txt");
        assertThat(parent).isEqualTo("folder1/folder2/");
    }

    @Test
    @DisplayName("Should extract filename")
    void shouldExtractFilename() {
        String filename = PathUtils.extractFilename("folder1/folder2/file.txt");
        assertThat(filename).isEqualTo("file.txt");
    }

    @Test
    @DisplayName("Should normalize path completely")
    void shouldNormalizePathCompletely() {
        String result = PathUtils.normalize("  folder1\\folder2\\  ");
        assertThat(result).isEqualTo("folder1/folder2");
    }

    @Test
    @DisplayName("Should normalize path with trailing slash")
    void shouldNormalizePathWithTrailingSlash() {
        String result = PathUtils.normalize("folder1/folder2/");
        assertThat(result).isEqualTo("folder1/folder2");
    }

    @Test
    @DisplayName("Should remove multiple trailing separators")
    void shouldRemoveMultipleTrailingSeparators() {
        String result = PathUtils.removeTrailingSeparator("folder///");
        assertThat(result).isEqualTo("folder");
    }

    @Test
    @DisplayName("Should normalize path with multiple trailing separators")
    void shouldNormalizePathWithMultipleTrailingSeparators() {
        String result = PathUtils.normalize("folder///");
        assertThat(result).isEqualTo("folder");
    }
}
