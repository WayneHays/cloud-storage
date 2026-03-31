package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.component.validator.MultipartFileValidator;
import com.waynehays.cloudfilestorage.config.properties.PathLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultipartFileValidatorTest {

    private final PathLimitsProperties properties = new PathLimitsProperties(
            500, 200);
    private final MultipartFileValidator validator = new MultipartFileValidator(properties);

    @Nested
    class ValidFiles {

        @Test
        void shouldPassForValidFile() {
            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate("document.txt", "directory/document.txt"));
        }

        @Test
        void shouldPassWhenFilenameIsExactlyMaxLength() {
            // given
            String originalFilename = "a".repeat(196) + ".txt";

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate(originalFilename, "dir/" + originalFilename));
        }

        @Test
        void shouldPassWhenFullPathIsExactlyMaxLength() {
            // given
            String directory = "a/".repeat(246);
            String originalFilename = "file.txt";

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate(originalFilename, directory + originalFilename));
        }
    }

    @Nested
    class BlankFilename {

        @Test
        void shouldThrowWhenFilenameIsNull() {
            // when & then
            assertThatThrownBy(() -> validator.validate(null, "directory/"))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenFilenameIsEmpty() {
            // when & then
            assertThatThrownBy(() -> validator.validate("","directory/"))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenFilenameIsBlank() {
            // when & then
            assertThatThrownBy(() -> validator.validate("   ", "directory/   "))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class FilenameLengthExceeded {

        @Test
        void shouldThrowWhenFilenameExceedsMaxLength() {
            // given
            String longName = "a".repeat(201) + ".txt";

            // when & then
            assertThatThrownBy(() -> validator.validate(longName, "directory/" + longName))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenNestedFilenameExceedsMaxLength() {
            // given
            String longName = "a".repeat(201) + ".txt";

            // when & then
            assertThatThrownBy(() -> validator.validate(longName, "directory/sub/dir/" + longName))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class OriginalFilenameLengthExceeded {

        @Test
        void shouldThrowWhenOriginalFilenameExceedsMaxPathLength() {
            // given
            String longPath = "a/".repeat(250) + "file.txt";

            // when & then
            assertThatThrownBy(() -> validator.validate(longPath, longPath))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class FullPathLengthExceeded {

        @Test
        void shouldThrowWhenFullPathExceedsMaxLength() {
            // given
            String longDirectory = "a/".repeat(250);

            // when & then
            assertThatThrownBy(() -> validator.validate("file.txt", longDirectory + "file.txt"))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }
}
