package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.infrastructure.path.ResourceLimitsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UploadRequestValidatorTest {

    @Mock
    private ResourceLimitsProperties properties;

    @InjectMocks
    private UploadRequestValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(properties.maxFilenameLength()).thenReturn(255);
        lenient().when(properties.maxPathLength()).thenReturn(1024);
        lenient().when(properties.maxFileSize()).thenReturn(DataSize.ofMegabytes(100));
    }

    private UploadObjectDto uploadObject(String filename, String fullPath, long size) {
        return new UploadObjectDto(filename, filename, "", fullPath, size,
                "text/plain", InputStream::nullInputStream);
    }

    @Nested
    class ValidateEachObject {

        @Test
        void shouldPass_whenAllObjectsAreValid() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("file.txt", "docs/file.txt", 100L)
            );

            // when & then
            assertThatNoException().isThrownBy(() -> validator.validate(objects));
        }

        @Test
        void shouldThrow_whenFilenameContainsInvalidCharacters() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("fi<le>.txt", "docs/fi<le>.txt", 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }

        @Test
        void shouldThrow_whenFilenameExceedsMaxLength() {
            // given
            String longFilename = "a".repeat(256) + ".txt";
            List<UploadObjectDto> objects = List.of(
                    uploadObject(longFilename, "docs/" + longFilename, 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }

        @Test
        void shouldThrow_whenFullPathExceedsMaxLength() {
            // given
            String longPath = "a/".repeat(513) + "file.txt";
            List<UploadObjectDto> objects = List.of(
                    uploadObject("file.txt", longPath, 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }

        @Test
        void shouldThrow_whenFileSizeExceedsMaxSize() {
            // given
            long oversizedBytes = DataSize.ofMegabytes(101).toBytes();
            List<UploadObjectDto> objects = List.of(
                    uploadObject("file.txt", "docs/file.txt", oversizedBytes)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }

        @Test
        void shouldCollectAllErrors_whenMultipleFilesAreInvalid() {
            // given
            String longFilename = "a".repeat(256) + ".txt";
            List<UploadObjectDto> objects = List.of(
                    uploadObject("fi<le>.txt", "docs/fi<le>.txt", 100L),
                    uploadObject(longFilename, "docs/" + longFilename, 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }
    }

    @Nested
    class ValidateNoDuplicates {

        @Test
        void shouldPass_whenNoDuplicatePaths() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("a.txt", "docs/a.txt", 100L),
                    uploadObject("b.txt", "docs/b.txt", 200L)
            );

            // when & then
            assertThatNoException().isThrownBy(() -> validator.validate(objects));
        }

        @Test
        void shouldThrow_whenDuplicateFullPathsExist() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("file.txt", "docs/file.txt", 100L),
                    uploadObject("file.txt", "docs/file.txt", 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void shouldThrow_whenMultipleDuplicatesExist() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("a.txt", "docs/a.txt", 100L),
                    uploadObject("a.txt", "docs/a.txt", 100L),
                    uploadObject("b.txt", "docs/b.txt", 100L),
                    uploadObject("b.txt", "docs/b.txt", 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Nested
    class ValidateOrder {

        @Test
        void shouldValidateEachObjectFirst_beforeCheckingDuplicates() {
            // given
            List<UploadObjectDto> objects = List.of(
                    uploadObject("fi<le>.txt", "docs/fi<le>.txt", 100L),
                    uploadObject("fi<le>.txt", "docs/fi<le>.txt", 100L)
            );

            // when & then
            assertThatThrownBy(() -> validator.validate(objects))
                    .isInstanceOf(UploadValidationException.class);
        }
    }
}
