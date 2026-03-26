package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.component.validator.MultipartFileValidator;
import com.waynehays.cloudfilestorage.config.properties.MultipartFileLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultipartFileValidatorTest {

    private MultipartFileValidator validator;

    @BeforeEach
    void setUp() {
        MultipartFileLimitsProperties properties = new MultipartFileLimitsProperties(500, 200);
        validator = new MultipartFileValidator(properties);
    }

    @Nested
    class ValidFiles {

        @Test
        void shouldPassForValidFile() {
            // given
            MultipartFile file = createMultipartFile("document.txt");

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate(file, "directory/document.txt"));
        }

        @Test
        void shouldPassWhenFilenameIsExactlyMaxLength() {
            // given
            String name = "a".repeat(196) + ".txt";
            MultipartFile file = createMultipartFile(name);

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate(file, "dir/" + name));
        }

        @Test
        void shouldPassWhenFullPathIsExactlyMaxLength() {
            // given
            String directory = "a/".repeat(246);
            String name = "file.txt";
            MultipartFile file = createMultipartFile(name);

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> validator.validate(file, directory + name));
        }
    }

    @Nested
    class BlankFilename {

        @Test
        void shouldThrowWhenFilenameIsNull() {
            // given
            MultipartFile file = createMultipartFile(null);

            // when & then
            assertThatThrownBy(() -> validator.validate(file, "directory/"))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenFilenameIsEmpty() {
            // given
            MultipartFile file = createMultipartFile("");

            // when & then
            assertThatThrownBy(() -> validator.validate(file, "directory/"))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenFilenameIsBlank() {
            // given
            MultipartFile file = createMultipartFile("   ");

            // when & then
            assertThatThrownBy(() -> validator.validate(file, "directory/   "))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class FilenameLengthExceeded {

        @Test
        void shouldThrowWhenFilenameExceedsMaxLength() {
            // given
            String longName = "a".repeat(201) + ".txt";
            MultipartFile file = createMultipartFile(longName);

            // when & then
            assertThatThrownBy(() -> validator.validate(file, "directory/" + longName))
                    .isInstanceOf(MultipartValidationException.class);
        }

        @Test
        void shouldThrowWhenNestedFilenameExceedsMaxLength() {
            // given
            String longName = "a".repeat(201) + ".txt";
            MultipartFile file = createMultipartFile("sub/dir/" + longName);

            // when & then
            assertThatThrownBy(() -> validator.validate(file, "directory/sub/dir/" + longName))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class OriginalFilenameLengthExceeded {

        @Test
        void shouldThrowWhenOriginalFilenameExceedsMaxPathLength() {
            // given
            String longPath = "a/".repeat(250) + "file.txt";
            MultipartFile file = createMultipartFile(longPath);

            // when & then
            assertThatThrownBy(() -> validator.validate(file, longPath))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    @Nested
    class FullPathLengthExceeded {

        @Test
        void shouldThrowWhenFullPathExceedsMaxLength() {
            // given
            String longDirectory = "a/".repeat(250);
            MultipartFile file = createMultipartFile("file.txt");

            // when & then
            assertThatThrownBy(() -> validator.validate(file, longDirectory + "file.txt"))
                    .isInstanceOf(MultipartValidationException.class);
        }
    }

    private MultipartFile createMultipartFile(String originalFilename) {
        return new MockMultipartFile("file", originalFilename, "text/plain", "content".getBytes());
    }
}
