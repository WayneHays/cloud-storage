package com.waynehays.cloudfilestorage.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationUtilsTest {

    @Nested
    class IsInvalidSegment {

        @ParameterizedTest
        @ValueSource(strings = {"file.txt", "docs", "my-file", "my_file", "Report 2024", "файл", "日本語"})
        @DisplayName("Should accept valid segments")
        void shouldAcceptValidSegments(String segment) {
            // when & then
            assertThat(ValidationUtils.isInvalidInput(segment)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"file.txt.bak", "v1.0", "name.ext"})
        @DisplayName("Should accept segments with dots in the middle")
        void shouldAcceptDotsInMiddle(String segment) {
            // when & then
            assertThat(ValidationUtils.isInvalidInput(segment)).isFalse();
        }

        @Test
        @DisplayName("Should reject blank segment")
        void shouldRejectBlank() {
            // when & then
            assertThat(ValidationUtils.isInvalidInput("")).isTrue();
            assertThat(ValidationUtils.isInvalidInput("   ")).isTrue();
        }

        @Test
        @DisplayName("Should reject current directory segment")
        void shouldRejectCurrentDir() {
            // when & then
            assertThat(ValidationUtils.isInvalidInput(".")).isTrue();
        }

        @Test
        @DisplayName("Should reject parent directory segment")
        void shouldRejectParentDir() {
            // when & then
            assertThat(ValidationUtils.isInvalidInput("..")).isTrue();
        }

        @Test
        @DisplayName("Should reject segment starting with dot")
        void shouldRejectStartingWithDot() {
            // when & then
            assertThat(ValidationUtils.isInvalidInput(".hidden")).isTrue();
        }

        @Test
        @DisplayName("Should reject segment ending with dot")
        void shouldRejectEndingWithDot() {
            // when & then
            assertThat(ValidationUtils.isInvalidInput("file.")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"file/name", "dir\\file", "test%file", "name@host", "file#1", "a&b", "a+b", "a=b"})
        @DisplayName("Should reject segments with special characters")
        void shouldRejectSpecialCharacters(String segment) {
            // when & then
            assertThat(ValidationUtils.isInvalidInput(segment)).isTrue();
        }
    }
}
