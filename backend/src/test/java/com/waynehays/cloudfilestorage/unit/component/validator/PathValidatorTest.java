package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import com.waynehays.cloudfilestorage.component.validator.PathValidator;
import com.waynehays.cloudfilestorage.config.properties.PathLimitsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PathValidatorTest {

    private final PathLimitsProperties limitsProperties = new PathLimitsProperties(500, 255);
    private final PathValidator validator = new PathValidator(limitsProperties);

    @Nested
    class GeneralPath {

        @BeforeEach
        void setUp() {
            initValidator(false);
        }

        @Test
        void shouldAcceptNull() {
            // when & then
            assertThat(validator.isValid(null, null)).isTrue();
        }

        @Test
        void shouldAcceptEmptyString() {
            // when & then
            assertThat(validator.isValid("", null)).isTrue();
        }

        @Test
        void shouldAcceptSimpleFilename() {
            // when & then
            assertThat(validator.isValid("file.txt", null)).isTrue();
        }

        @Test
        void shouldAcceptNestedPath() {
            // when & then
            assertThat(validator.isValid("directory/subdirectory/file.txt", null)).isTrue();
        }

        @Test
        void shouldAcceptPathWithTrailingSlash() {
            // when & then
            assertThat(validator.isValid("directory/subdirectory/", null)).isTrue();
        }

        @Test
        void shouldAcceptUnicodeCharacters() {
            // when & then
            assertThat(validator.isValid("документы/файл.txt", null)).isTrue();
        }

        @Test
        void shouldAcceptHyphensUnderscoresSpaces() {
            // when & then
            assertThat(validator.isValid("my-dir/my_file name.txt", null)).isTrue();
        }

        @Test
        void shouldRejectDoubleSlash() {
            // when & then
            assertThat(validator.isValid("directory//file.txt", null)).isFalse();
        }

        @Test
        void shouldRejectParentDirectoryTraversal() {
            // when & then
            assertThat(validator.isValid("directory/../file.txt", null)).isFalse();
        }

        @Test
        void shouldRejectCurrentDirectoryReference() {
            // when & then
            assertThat(validator.isValid("directory/./file.txt", null)).isFalse();
        }

        @Test
        void shouldRejectSegmentStartingWithDot() {
            // when & then
            assertThat(validator.isValid("directory/.hidden", null)).isFalse();
        }

        @Test
        void shouldRejectSegmentEndingWithDot() {
            // when & then
            assertThat(validator.isValid("directory/file.", null)).isFalse();
        }

        @Test
        void shouldRejectSpecialCharacters() {
            // when & then
            assertThat(validator.isValid("directory/file@name.txt", null)).isFalse();
        }

        @Test
        void shouldRejectLeadingSlash() {
            // when & then
            assertThat(validator.isValid("/directory/file.txt", null)).isFalse();
        }
    }

    @Nested
    class DirectoryPath {

        @BeforeEach
        void setUp() {
            initValidator(true);
        }

        @Test
        void shouldAcceptDirectoryWithTrailingSlash() {
            // when & then
            assertThat(validator.isValid("directory/subdirectory/", null)).isTrue();
        }

        @Test
        void shouldRejectDirectoryWithoutTrailingSlash() {
            // when & then
            assertThat(validator.isValid("directory/subdirectory", null)).isFalse();
        }

        @Test
        void shouldAcceptNullForDirectory() {
            // when & then
            assertThat(validator.isValid(null, null)).isTrue();
        }

        @Test
        void shouldAcceptEmptyForDirectory() {
            // when & then
            assertThat(validator.isValid("", null)).isTrue();
        }
    }

    private void initValidator(boolean mustBeDirectory) {
        ValidPath annotation = mock(ValidPath.class);
        when(annotation.mustBeDirectory()).thenReturn(mustBeDirectory);
        validator.initialize(annotation);
    }
}
