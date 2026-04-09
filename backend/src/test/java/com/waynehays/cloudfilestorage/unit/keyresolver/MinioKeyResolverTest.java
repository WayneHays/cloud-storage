package com.waynehays.cloudfilestorage.unit.component.keyresolver;

import com.waynehays.cloudfilestorage.storage.minio.MinioKeyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MinioKeyResolverTest {

    private final MinioKeyResolver keyResolver = new MinioKeyResolver();

    @Nested
    class GenerateKeyTest {

        @Test
        @DisplayName("Should generate key with parentPath")
        void shouldGenerateKeyWithDirectory() {
            String key = keyResolver.resolveKey(1L, "docs/file.txt");

            assertThat(key).isEqualTo("user-1-files/docs/file.txt");
        }

        @Test
        @DisplayName("Should generate key for root file")
        void shouldGenerateKeyForRootFile() {
            String key = keyResolver.resolveKey(1L, "file.txt");

            assertThat(key).isEqualTo("user-1-files/file.txt");
        }

        @Test
        @DisplayName("Should generate key with nested parentPath")
        void shouldGenerateKeyWithNestedDirectory() {
            String key = keyResolver.resolveKey(1L, "docs/work/task/file.txt");

            assertThat(key).isEqualTo("user-1-files/docs/work/task/file.txt");
        }

        @Test
        @DisplayName("Should include user id in key")
        void shouldIncludeUserId() {
            String key = keyResolver.resolveKey(42L, "file.txt");

            assertThat(key).startsWith("user-42-files/");
        }
    }

    @Nested
    class ExtractPathTest {

        private static Stream<Arguments> storageKeyAndExpectedResult() {
            return Stream.of(
                    Arguments.of("user-1-files/docs/work/a.txt", "docs/work/a.txt"),
                    Arguments.of("user-1-files/a.txt", "a.txt"),
                    Arguments.of("user-1-files/docs/work/", "docs/work/"));
        }

        @ParameterizedTest
        @MethodSource("storageKeyAndExpectedResult")
        void shouldExtractPathFromStorageKey(String objectKey, String expectedResult) {
            // when
            assertThat(keyResolver.extractPath(1L, objectKey)).isEqualTo(expectedResult);
        }
    }
}
