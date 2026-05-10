package com.waynehays.cloudfilestorage.files.api.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentDispositionBuilderTest {

    private final ContentDispositionBuilder builder = new ContentDispositionBuilder();

    @Test
    @DisplayName("Should build header for ASCII filename")
    void shouldBuildHeaderForAsciiFilename() {
        String result = builder.contentDisposition("file.txt");

        assertThat(result).isEqualTo("attachment; filename=\"file.txt\"; filename*=UTF-8''file.txt");
    }

    @Test
    @DisplayName("Should replace non-ASCII chars with underscores in fallback")
    void shouldReplaceNonAsciiInFallback() {
        String result = builder.contentDisposition("файл.txt");

        assertThat(result).contains("filename=\"____.txt\"");
    }

    @Test
    @DisplayName("Should encode non-ASCII chars in filename* parameter")
    void shouldEncodeNonAsciiInFilenameStar() {
        String result = builder.contentDisposition("файл.txt");

        assertThat(result).contains("filename*=UTF-8''%D1%84%D0%B0%D0%B9%D0%BB.txt");
    }

    @Test
    @DisplayName("Should escape double quotes in ASCII fallback")
    void shouldEscapeDoubleQuotesInFallback() {
        String result = builder.contentDisposition("a\"b.txt");

        assertThat(result).contains("filename=\"a\\\"b.txt\"");
    }

    @Test
    @DisplayName("Should encode spaces as %20, not as +")
    void shouldEncodeSpacesAsPercent20() {
        String result = builder.contentDisposition("my file.txt");

        assertThat(result).contains("filename*=UTF-8''my%20file.txt");
    }

    @Test
    @DisplayName("Should keep ASCII punctuation in fallback")
    void shouldKeepAsciiPunctuationInFallback() {
        String result = builder.contentDisposition("file (1).txt");

        assertThat(result).contains("filename=\"file (1).txt\"");
    }
}