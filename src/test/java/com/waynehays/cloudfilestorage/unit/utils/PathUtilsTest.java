package com.waynehays.cloudfilestorage.unit.utils;

import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Nested
    class IsDirectory {

        @ParameterizedTest
        @ValueSource(strings = {"directory/", "directory/subdirectory/", "a/"})
        void shouldReturnTrueForPathWithTrailingSlash(String path) {
            // given & when & then
            assertThat(PathUtils.isDirectory(path)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"directory", "directory/file.txt", "file.txt"})
        void shouldReturnFalseForPathWithoutTrailingSlash(String path) {
            // given & when & then
            assertThat(PathUtils.isDirectory(path)).isFalse();
        }
    }

    @Nested
    class IsFile {

        @ParameterizedTest
        @ValueSource(strings = {"directory/file.txt", "file.txt", "directory"})
        void shouldReturnTrueForPathWithoutTrailingSlash(String path) {
            // given & when & then
            assertThat(PathUtils.isFile(path)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"directory/", "directory/subdirectory/", "a/"})
        void shouldReturnFalseForPathWithTrailingSlash(String path) {
            // given & when & then
            assertThat(PathUtils.isFile(path)).isFalse();
        }
    }

    @Nested
    class GetAllDirectories {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory1/directory2/directory3 | directory1,directory1/directory2,directory1/directory2/directory3",
                "directory1/directory2/        | directory1,directory1/directory2",
                "directory1                 | directory1"
        })
        void shouldReturnAllDirectoryLevels(String path, String expected) {
            // given
            List<String> expectedList = List.of(expected.split(","));

            // when
            List<String> result = PathUtils.getAllDirectories(path);

            // then
            assertThat(result).containsExactlyElementsOf(expectedList);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldReturnEmptyListForBlankOrNull(String path) {
            // given & when
            List<String> result = PathUtils.getAllDirectories(path);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class NormalizeSeparators {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory\\subdirectory\\file.txt | directory/subdirectory/file.txt",
                "directory/subdirectory/file.txt   | directory/subdirectory/file.txt",
                "file.txt                    | file.txt"
        })
        void shouldNormalizeSeparators(String input, String expected) {
            // given & when
            String result = PathUtils.normalizeSeparators(input);

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class RemoveTrailingSeparator {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory/subdirectory/ | directory/subdirectory",
                "directory/file.txt  | directory/file.txt",
                "directory/          | directory"
        })
        void shouldRemoveTrailingSeparator(String input, String expected) {
            // given & when
            String result = PathUtils.removeTrailingSlash(input);

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class ExtractParentPath {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory1/directory2/file.txt | directory1/directory2/",
                "directory1/directory2/         | directory1/"
        })
        void shouldExtractParentPath(String input, String expected) {
            // given & when
            String result = PathUtils.extractParentPath(input);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "file.txt",
                "directory/"
        })
        void shouldReturnEmptyForRootLevel(String input) {
            // given & when
            String result = PathUtils.extractParentPath(input);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExtractFilename {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory1/directory2/file.txt | file.txt",
                "directory1/directory2/         | directory2",
                "file.txt                 | file.txt"
        })
        void shouldExtractFilename(String input, String expected) {
            // given & when
            String result = PathUtils.extractFilename(input);

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class Combine {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "directory1  | file.txt   | directory1/file.txt",
                "directory1/ | file.txt   | directory1/file.txt",
                "directory1  | subdirectory/ | directory1/subdirectory"
        })
        void shouldCombinePaths(String base, String sub, String expected) {
            // given & when
            String result = PathUtils.combine(base, sub);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldReturnSubWhenBaseIsEmpty() {
            // given & when
            String result = PathUtils.combine("", "file.txt");

            // then
            assertThat(result).isEqualTo("file.txt");
        }

        @Test
        void shouldReturnBaseWhenSubIsEmpty() {
            // given & when
            String result = PathUtils.combine("directory1", "");

            // then
            assertThat(result).isEqualTo("directory1");
        }

        @Test
        void shouldReturnEmptyWhenBothEmpty() {
            // given & when
            String result = PathUtils.combine("", "");

            // then
            assertThat(result).isEmpty();
        }
    }
}
